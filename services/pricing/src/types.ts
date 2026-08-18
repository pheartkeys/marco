export enum QuoteConfidence {
  KNOWN = 'KNOWN',
  ESTIMATED = 'ESTIMATED',
  MODELED = 'MODELED',
  UNKNOWN = 'UNKNOWN'
}

export interface QuoteRequest {
  category: string; // LODGING, FLIGHT, DINING, TRANSPORT, ACTIVITY, FX
  locality?: string;
  tier?: string;
  dateIso?: string;
  partySize?: number;
  preferredCurrency?: string;
}

export interface PriceQuote {
  amount: number;
  currency: string;
  source: string;
  fetchedAtTimestamp: number;
  confidence: QuoteConfidence;
  asOfLabel?: string;
}

export type QuoteResult =
  | { status: 'AVAILABLE'; quote: PriceQuote }
  | { status: 'UNAVAILABLE'; reason: string };
