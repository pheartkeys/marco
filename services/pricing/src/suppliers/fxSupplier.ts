import { PriceQuote, QuoteConfidence, QuoteRequest, QuoteResult } from '../types';

// Curated reference rates with timestamp
const BASE_FX_RATES_TO_USD: Record<string, number> = {
  USD: 1.0,
  EUR: 1.08,
  GBP: 1.28,
  JPY: 0.0067,
  CAD: 0.74,
  CHF: 1.13,
  AUD: 0.65,
  SGD: 0.75
};

export async function fetchFxQuote(request: QuoteRequest): Promise<QuoteResult> {
  const targetCurrency = (request.preferredCurrency || request.locality || 'USD').toUpperCase();
  const baseCurrency = (request.tier || 'USD').toUpperCase();

  const baseToUsd = BASE_FX_RATES_TO_USD[baseCurrency];
  const targetToUsd = BASE_FX_RATES_TO_USD[targetCurrency];

  if (!baseToUsd || !targetToUsd) {
    return {
      status: 'UNAVAILABLE',
      reason: `Currency pair ${baseCurrency}/${targetCurrency} is not supported.`
    };
  }

  // 1 unit of baseCurrency in targetCurrency
  const rate = baseToUsd / targetToUsd;

  return {
    status: 'AVAILABLE',
    quote: {
      amount: Math.round(rate * 10000) / 10000,
      currency: targetCurrency,
      source: 'PRICING_SERVICE_FX',
      fetchedAtTimestamp: Date.now(),
      confidence: QuoteConfidence.ESTIMATED,
      asOfLabel: new Date().toLocaleDateString('en-US', { month: 'short', year: 'numeric' })
    }
  };
}
