import { PriceQuote, QuoteConfidence, QuoteRequest, QuoteResult } from '../types';

// Amadeus baseline estimates for flight & lodging tiers per room/seat
const AMADEUS_TRANSPORT_TIERS: Record<string, number> = {
  bicycle: 25.0,
  rail: 85.0,
  economy: 350.0,
  economy_flight: 350.0,
  business: 1800.0,
  business_flight: 1800.0,
  charter: 5500.0,
  private_jet: 18000.0
};

const AMADEUS_LODGING_TIERS: Record<string, number> = {
  hostel: 45.0,
  timeshare: 180.0,
  boutique: 280.0,
  boutique_hotel: 280.0,
  resort: 480.0,
  luxury_resort: 480.0,
  villa: 950.0,
  private_villa: 950.0
};

export async function fetchAmadeusQuote(request: QuoteRequest): Promise<QuoteResult> {
  const category = request.category.toUpperCase();
  const tier = (request.tier || '').toLowerCase().replace(/\s+/g, '_');
  const partySize = Math.max(1, request.partySize || 1);

  if (category === 'FLIGHT' || category === 'TRANSPORT') {
    const basePerPerson = AMADEUS_TRANSPORT_TIERS[tier] || 400.0;
    const total = basePerPerson * partySize;
    return {
      status: 'AVAILABLE',
      quote: {
        amount: total,
        currency: request.preferredCurrency || 'USD',
        source: 'AMADEUS_SELF_SERVICE',
        fetchedAtTimestamp: Date.now(),
        confidence: QuoteConfidence.ESTIMATED,
        asOfLabel: `Amadeus ${tier.replace('_', ' ')} benchmark`
      }
    };
  }

  if (category === 'LODGING') {
    const rooms = Math.ceil(partySize / 2);
    const basePerRoom = AMADEUS_LODGING_TIERS[tier] || 250.0;
    const total = basePerRoom * rooms;
    return {
      status: 'AVAILABLE',
      quote: {
        amount: total,
        currency: request.preferredCurrency || 'USD',
        source: 'AMADEUS_SELF_SERVICE',
        fetchedAtTimestamp: Date.now(),
        confidence: QuoteConfidence.ESTIMATED,
        asOfLabel: `Amadeus ${tier.replace('_', ' ')} benchmark`
      }
    };
  }

  return {
    status: 'UNAVAILABLE',
    reason: `Amadeus supplier cannot quote category ${request.category}`
  };
}
