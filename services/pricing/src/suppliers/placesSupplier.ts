import { PriceQuote, QuoteConfidence, QuoteRequest, QuoteResult } from '../types';

// Google Places price level tier estimates per person (in USD)
const PLACES_DINING_TIERS: Record<string, number> = {
  street: 15.0,
  street_food: 15.0,
  casual: 35.0,
  casual_dining: 35.0,
  notable: 85.0,
  notable_bistro: 85.0,
  fine: 220.0,
  fine_dining: 220.0
};

const PLACES_ACTIVITY_TIERS: Record<string, number> = {
  museum: 25.0,
  guided_tour: 65.0,
  adventure: 150.0,
  excursion: 120.0,
  wellness: 180.0
};

export async function fetchPlacesQuote(request: QuoteRequest): Promise<QuoteResult> {
  const category = request.category.toUpperCase();
  const tier = (request.tier || '').toLowerCase().replace(/\s+/g, '_');
  const partySize = Math.max(1, request.partySize || 1);

  if (category === 'DINING') {
    const basePerPerson = PLACES_DINING_TIERS[tier] || 45.0;
    const total = basePerPerson * partySize;
    return {
      status: 'AVAILABLE',
      quote: {
        amount: total,
        currency: request.preferredCurrency || 'USD',
        source: 'GOOGLE_PLACES',
        fetchedAtTimestamp: Date.now(),
        confidence: QuoteConfidence.ESTIMATED,
        asOfLabel: `${tier.replace('_', ' ')} tier estimate`
      }
    };
  }

  if (category === 'ACTIVITY') {
    const basePerPerson = PLACES_ACTIVITY_TIERS[tier] || 50.0;
    const total = basePerPerson * partySize;
    return {
      status: 'AVAILABLE',
      quote: {
        amount: total,
        currency: request.preferredCurrency || 'USD',
        source: 'GOOGLE_PLACES',
        fetchedAtTimestamp: Date.now(),
        confidence: QuoteConfidence.ESTIMATED,
        asOfLabel: `${tier.replace('_', ' ')} activity estimate`
      }
    };
  }

  return {
    status: 'UNAVAILABLE',
    reason: `Places supplier cannot estimate category ${request.category}`
  };
}
