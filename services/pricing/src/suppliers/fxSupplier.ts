import { PriceQuote, QuoteConfidence, QuoteRequest, QuoteResult } from '../types';

/**
 * FX supplier — a real live call to a currency-conversion API (exchangerate-api.com's v6 pair
 * endpoint), never a hardcoded rate table. If FX_API_KEY is unset, or the live call fails or
 * returns something unusable, this reports UNAVAILABLE with a reason. It never falls back to a
 * synthesized rate: a stale or guessed exchange rate is worse than an honest gap, because a
 * silently wrong rate corrupts every downstream conversion that trusts it.
 */
const EXCHANGERATE_API_BASE = 'https://v6.exchangerate-api.com/v6';

export async function fetchFxQuote(request: QuoteRequest): Promise<QuoteResult> {
  const targetCurrency = (request.preferredCurrency || request.locality || '').toUpperCase();
  const baseCurrency = (request.tier || '').toUpperCase();

  if (!targetCurrency || !baseCurrency) {
    return {
      status: 'UNAVAILABLE',
      reason: 'FX quote requires both a base currency (tier) and a target currency (preferredCurrency).'
    };
  }

  const apiKey = process.env.FX_API_KEY;
  if (!apiKey) {
    return { status: 'UNAVAILABLE', reason: 'FX supplier is not configured (FX_API_KEY missing).' };
  }

  try {
    const response = await fetch(`${EXCHANGERATE_API_BASE}/${apiKey}/pair/${baseCurrency}/${targetCurrency}`);
    if (!response.ok) {
      return { status: 'UNAVAILABLE', reason: `FX supplier returned HTTP ${response.status}.` };
    }

    const data: any = await response.json();
    if (data.result !== 'success' || typeof data.conversion_rate !== 'number') {
      const errorType = typeof data['error-type'] === 'string' ? data['error-type'] : 'no usable rate';
      return { status: 'UNAVAILABLE', reason: `FX supplier error: ${errorType}.` };
    }

    const quote: PriceQuote = {
      amount: data.conversion_rate,
      currency: targetCurrency,
      source: 'EXCHANGERATE_API',
      fetchedAtTimestamp: Date.now(),
      confidence: QuoteConfidence.ESTIMATED,
      asOfLabel: typeof data.time_last_update_utc === 'string' ? data.time_last_update_utc : ''
    };
    return { status: 'AVAILABLE', quote };
  } catch (err: any) {
    return { status: 'UNAVAILABLE', reason: `FX supplier request failed: ${err?.message || 'network error'}` };
  }
}
