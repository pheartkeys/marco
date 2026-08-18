import { PriceQuote, QuoteConfidence, QuoteRequest, QuoteResult } from '../types';

/**
 * Amadeus supplier — real OAuth2 client-credentials auth against the Amadeus for Developers
 * self-service API, then a real city-search -> hotel-list -> hotel-offers pipeline for LODGING.
 * Never a hardcoded per-tier price table. If AMADEUS_API_KEY/AMADEUS_API_SECRET are unset, auth
 * fails, or any step of the live pipeline fails or returns nothing priced, this reports
 * UNAVAILABLE with a reason rather than substituting a plausible number.
 *
 * FLIGHT and TRANSPORT are intentionally NOT quoted here: Amadeus Flight Offers Search requires
 * an origin, and `QuoteRequest` (services/pricing/src/types.ts, mirrored by
 * app/src/main/java/com/example/data/model/PricingModels.kt, which this track may not edit this
 * round) carries only a single `locality` — no origin field. Rather than guess an origin or
 * revive the old per-tier table, this supplier reports the gap honestly. See this track's report
 * for the recommended follow-up (adding an optional origin field to the request contract).
 */
const AMADEUS_BASE_URL = 'https://test.api.amadeus.com';

async function getAmadeusAccessToken(apiKey: string, apiSecret: string): Promise<string | null> {
  try {
    const response = await fetch(`${AMADEUS_BASE_URL}/v1/security/oauth2/token`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: new URLSearchParams({
        grant_type: 'client_credentials',
        client_id: apiKey,
        client_secret: apiSecret
      }).toString()
    });
    if (!response.ok) return null;
    const data: any = await response.json();
    return typeof data.access_token === 'string' ? data.access_token : null;
  } catch {
    return null;
  }
}

async function resolveCityCode(locality: string, token: string): Promise<string | null> {
  try {
    const url = new URL(`${AMADEUS_BASE_URL}/v1/reference-data/locations`);
    url.searchParams.set('subType', 'CITY');
    url.searchParams.set('keyword', locality);
    const response = await fetch(url.toString(), { headers: { Authorization: `Bearer ${token}` } });
    if (!response.ok) return null;
    const data: any = await response.json();
    const first = Array.isArray(data.data) ? data.data[0] : undefined;
    return typeof first?.iataCode === 'string' ? first.iataCode : null;
  } catch {
    return null;
  }
}

async function fetchHotelQuote(request: QuoteRequest, token: string): Promise<QuoteResult> {
  const locality = request.locality || '';
  if (!locality) {
    return { status: 'UNAVAILABLE', reason: 'Lodging quote requires a locality.' };
  }

  const cityCode = await resolveCityCode(locality, token);
  if (!cityCode) {
    return { status: 'UNAVAILABLE', reason: `Amadeus could not resolve a city code for "${locality}".` };
  }

  try {
    const hotelsUrl = new URL(`${AMADEUS_BASE_URL}/v1/reference-data/locations/hotels/by-city`);
    hotelsUrl.searchParams.set('cityCode', cityCode);
    const hotelsResponse = await fetch(hotelsUrl.toString(), { headers: { Authorization: `Bearer ${token}` } });
    if (!hotelsResponse.ok) {
      return { status: 'UNAVAILABLE', reason: `Amadeus hotel list returned HTTP ${hotelsResponse.status}.` };
    }
    const hotelsData: any = await hotelsResponse.json();
    const hotelIds = (hotelsData.data || [])
      .slice(0, 20)
      .map((h: any) => h.hotelId)
      .filter((id: any): id is string => typeof id === 'string');
    if (hotelIds.length === 0) {
      return { status: 'UNAVAILABLE', reason: `Amadeus found no hotels for "${locality}".` };
    }

    const offersUrl = new URL(`${AMADEUS_BASE_URL}/v3/shopping/hotel-offers`);
    offersUrl.searchParams.set('hotelIds', hotelIds.join(','));
    if (request.dateIso) offersUrl.searchParams.set('checkInDate', request.dateIso);
    offersUrl.searchParams.set('adults', String(Math.max(1, request.partySize || 1)));
    const offersResponse = await fetch(offersUrl.toString(), { headers: { Authorization: `Bearer ${token}` } });
    if (!offersResponse.ok) {
      return { status: 'UNAVAILABLE', reason: `Amadeus hotel offers returned HTTP ${offersResponse.status}.` };
    }
    const offersData: any = await offersResponse.json();
    const offer = (offersData.data || [])
      .flatMap((h: any) => h.offers || [])
      .find((o: any) => o?.price?.total);
    if (!offer) {
      return { status: 'UNAVAILABLE', reason: `Amadeus returned no priced hotel offers for "${locality}".` };
    }

    const rawCurrency = typeof offer.price.currency === 'string' ? offer.price.currency.trim() : '';
    if (!rawCurrency) {
      return { status: 'UNAVAILABLE', reason: `Amadeus offer for "${locality}" omitted currency.` };
    }

    const amount = parseFloat(offer.price.total);
    if (isNaN(amount) || amount <= 0) {
      return { status: 'UNAVAILABLE', reason: `Amadeus offer for "${locality}" returned invalid price total.` };
    }

    const quote: PriceQuote = {
      amount,
      currency: rawCurrency,
      source: 'AMADEUS',
      fetchedAtTimestamp: Date.now(),
      confidence: QuoteConfidence.ESTIMATED,
      asOfLabel: `Amadeus live hotel offer, ${cityCode}`
    };
    return { status: 'AVAILABLE', quote };
  } catch (err: any) {
    return { status: 'UNAVAILABLE', reason: `Amadeus lodging request failed: ${err?.message || 'network error'}` };
  }
}

export async function fetchAmadeusQuote(request: QuoteRequest): Promise<QuoteResult> {
  const category = request.category.toUpperCase();

  if (category !== 'LODGING' && category !== 'FLIGHT' && category !== 'TRANSPORT') {
    return { status: 'UNAVAILABLE', reason: `Amadeus supplier cannot quote category ${request.category}` };
  }

  if (category === 'FLIGHT' || category === 'TRANSPORT') {
    return {
      status: 'UNAVAILABLE',
      reason:
        `Amadeus cannot price ${category.toLowerCase()} without an origin, which the current ` +
        'quote request does not carry.'
    };
  }

  const apiKey = process.env.AMADEUS_API_KEY;
  const apiSecret = process.env.AMADEUS_API_SECRET;
  if (!apiKey || !apiSecret) {
    return {
      status: 'UNAVAILABLE',
      reason: 'Amadeus supplier is not configured (AMADEUS_API_KEY/AMADEUS_API_SECRET missing).'
    };
  }

  const token = await getAmadeusAccessToken(apiKey, apiSecret);
  if (!token) {
    return { status: 'UNAVAILABLE', reason: 'Amadeus authentication failed.' };
  }

  return fetchHotelQuote(request, token);
}
