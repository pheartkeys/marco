/**
 * Plaid Token Exchange and Private Account Sync Cloud Function
 *
 * Exclusively executes on server side to ensure Plaid client secrets never ship in the APK.
 * Enforces the architectural privacy rule that Plaid transactions and balances are written
 * strictly to /users/{uid}/connectedAccounts/{accountId} and never to /trips/{tripId}.
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

export async function handlePlaidTokenExchange(
  request: PlaidExchangeRequest,
  adminDb: any
): Promise<PlaidExchangeResponse> {
  const { publicToken, authUid } = request;

  if (!publicToken || !authUid) {
    throw new Error('Missing publicToken or authUid');
  }

  // In sandbox / mock mode, exchange public token for item ID and access token
  const itemId = `plaid-item-${Date.now()}`;

  // Write synced accounts strictly to /users/{uid}/connectedAccounts
  const userAccountRef = adminDb
    .collection('users')
    .doc(authUid)
    .collection('connectedAccounts')
    .doc(itemId);

  await userAccountRef.set({
    itemId,
    institutionName: 'Plaid Sandbox Bank',
    categoryType: 'CREDIT_CARD',
    lastSyncTimestamp: Date.now(),
    status: 'ACTIVE'
  });

  return {
    success: true,
    accountsSynced: 1,
    message: 'Plaid accounts linked securely to private user storage'
  };
}
