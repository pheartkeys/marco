/**
 * Plaid Token Exchange and Private Account Sync Cloud Function
 *
 * Exclusively executes on server side to ensure Plaid client secrets never ship in the APK.
 * Enforces the architectural privacy rule that Plaid transactions and balances are written
 * strictly to /users/{uid}/connectedAccounts/{accountId} and never to /trips/{tripId}.
 *
 * Calls Plaid's real Sandbox environment (https://sandbox.plaid.com) — a live API with test
 * institutions, not a locally-synthesized account. If PLAID_CLIENT_ID/PLAID_SANDBOX_SECRET are
 * unset or any Plaid call fails, this throws rather than writing a fabricated linked account.
 * Scope stays strictly to accounts/balances (US, sandbox) — never Auth, Identity, or Liabilities.
 */

export interface PlaidExchangeRequest {
  publicToken: string;
  authUid: string;
}

export interface PlaidExchangeResponse {
  success: boolean;
  accountsSynced: number;
  message?: string;
}

const PLAID_SANDBOX_BASE_URL = 'https://sandbox.plaid.com';

export async function handlePlaidTokenExchange(
  request: PlaidExchangeRequest,
  adminDb: any
): Promise<PlaidExchangeResponse> {
  const { publicToken, authUid } = request;

  if (!publicToken || !authUid) {
    throw new Error('Missing publicToken or authUid');
  }

  const clientId = process.env.PLAID_CLIENT_ID;
  const secret = process.env.PLAID_SANDBOX_SECRET;
  if (!clientId || !secret) {
    throw new Error('Plaid is not configured (PLAID_CLIENT_ID/PLAID_SANDBOX_SECRET missing).');
  }

  // Exchange the public token for a real Plaid sandbox access token / item id.
  const exchangeResponse = await fetch(`${PLAID_SANDBOX_BASE_URL}/item/public_token/exchange`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ client_id: clientId, secret, public_token: publicToken })
  });
  if (!exchangeResponse.ok) {
    throw new Error(`Plaid token exchange failed with HTTP ${exchangeResponse.status}`);
  }
  const exchangeData: any = await exchangeResponse.json();
  const accessToken = exchangeData.access_token;
  const itemId = exchangeData.item_id;
  if (!accessToken || !itemId) {
    throw new Error('Plaid token exchange returned no access token.');
  }

  // Balances only — this function never calls Plaid Auth/Identity/Liabilities/Investments.
  const balancesResponse = await fetch(`${PLAID_SANDBOX_BASE_URL}/accounts/balance/get`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ client_id: clientId, secret, access_token: accessToken })
  });
  if (!balancesResponse.ok) {
    throw new Error(`Plaid balances fetch failed with HTTP ${balancesResponse.status}`);
  }
  const balancesData: any = await balancesResponse.json();
  const accounts = Array.isArray(balancesData.accounts) ? balancesData.accounts : [];
  const institutionId =
    typeof balancesData.item?.institution_id === 'string' ? balancesData.item.institution_id : '';

  // Write strictly to /users/{uid}/connectedAccounts — never /trips.
  const userAccountsRef = adminDb.collection('users').doc(authUid).collection('connectedAccounts');
  for (const account of accounts) {
    if (!account?.account_id) continue;
    await userAccountsRef.doc(account.account_id).set({
      itemId,
      institutionId,
      accountName: account.name || '',
      accountSubtype: account.subtype || '',
      availableBalance: account.balances?.available ?? null,
      currentBalance: account.balances?.current ?? null,
      isoCurrencyCode: account.balances?.iso_currency_code || '',
      countryCode: 'US',
      environment: 'sandbox',
      lastSyncTimestamp: Date.now(),
      status: 'ACTIVE'
    });
  }

  return {
    success: true,
    accountsSynced: accounts.length,
    message:
      accounts.length > 0
        ? 'Plaid accounts linked to private user storage.'
        : 'Plaid link succeeded but returned no accounts.'
  };
}
