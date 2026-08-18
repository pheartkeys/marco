import { PriceQuote, QuoteConfidence, QuoteRequest, QuoteResult } from '../types';

/**
 * Places supplier — a real live call to the Google Places Text Search API, never a hardcoded
 * per-tier price table. If GOOGLE_PLACES_API_KEY is unset, the call fails, or no venue in the
 * response carries a `price_level`, this reports UNAVAILABLE with a reason rather than
 * substituting a plausible number.
 *
 * Google Places does not return a dollar figure for a venue — only a `price_level` bucket
 * (0 = free … 4 = very expensive), per Google's own published semantics. Converting that bucket
 * into an approximate USD-per-person figure is an unavoidable translation step, but it is only
 * ever applied to a `price_level` a live call actually returned for a real venue matching the
 * request — never used as a fallback when the call fails or returns nothing.
 */
const PRICE_LEVEL_USD_PER_PERSON: Record<number, number> = {
  0: 15.0,
  1: 25.0,
  2: 55.0,
  3: 110.0,
  4: 220.0
};

const PLACES_TEXT_SEARCH_URL = 'https://maps.googleapis.com/maps/api/place/textsearch/json';

export async function fetchPlacesQuote(request: QuoteRequest): Promise<QuoteResult> {
  const category = request.category.toUpperCase();
  if (category !== 'DINING' && category !== 'ACTIVITY') {
    return { status: 'UNAVAILABLE', reason: `Places supplier cannot quote category ${request.category}` };
  }

  const apiKey = process.env.GOOGLE_PLACES_API_KEY;
  if (!apiKey) {
    return { status: 'UNAVAILABLE', reason: 'Places supplier is not configured (GOOGLE_PLACES_API_KEY missing).' };
  }

  const tier = (request.tier || '').replace(/_/g, ' ').trim();
  const locality = request.locality || '';
  const kind = category === 'DINING' ? 'restaurant' : 'activity';
  const query = [tier, kind, locality].filter(Boolean).join(' ') || kind;
  const partySize = Math.max(1, request.partySize || 1);

  try {
    const url = new URL(PLACES_TEXT_SEARCH_URL);
    url.searchParams.set('query', query);
    url.searchParams.set('key', apiKey);

    const response = await fetch(url.toString());
    if (!response.ok) {
      return { status: 'UNAVAILABLE', reason: `Places supplier returned HTTP ${response.status}.` };
    }

    const data: any = await response.json();
    if (data.status !== 'OK' || !Array.isArray(data.results) || data.results.length === 0) {
      return { status: 'UNAVAILABLE', reason: `Places supplier found no venues for "${query}".` };
    }

    const top = data.results.find((r: any) => typeof r.price_level === 'number');
    if (!top) {
      return {
        status: 'UNAVAILABLE',
        reason: `Places supplier found venues for "${query}" but none reported a price level.`
      };
    }

    const perPerson = PRICE_LEVEL_USD_PER_PERSON[top.price_level];
    const quote: PriceQuote = {
      amount: perPerson * partySize,
      currency: 'USD',
      source: 'GOOGLE_PLACES',
      fetchedAtTimestamp: Date.now(),
      confidence: QuoteConfidence.ESTIMATED,
      asOfLabel: `${top.name || 'venue'} price level ${top.price_level}`
    };
    return { status: 'AVAILABLE', quote };
  } catch (err: any) {
    return { status: 'UNAVAILABLE', reason: `Places supplier request failed: ${err?.message || 'network error'}` };
  }
}
