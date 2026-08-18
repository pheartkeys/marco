import { fetchAmadeusQuote } from './suppliers/amadeusSupplier';
import { fetchPlacesQuote } from './suppliers/placesSupplier';
import { fetchFxQuote } from './suppliers/fxSupplier';
import { QuoteRequest } from './types';

describe('Pricing Service Suppliers - Zero Fabrication and Missing Key Tests', () => {
  const originalEnv = process.env;

  beforeEach(() => {
    jest.resetModules();
    process.env = { ...originalEnv };
    delete process.env.AMADEUS_API_KEY;
    delete process.env.AMADEUS_API_SECRET;
    delete process.env.GOOGLE_PLACES_API_KEY;
    delete process.env.FX_API_KEY;
  });

  afterAll(() => {
    process.env = originalEnv;
  });

  test('Amadeus supplier returns UNAVAILABLE when API keys are missing', async () => {
    const request: QuoteRequest = {
      category: 'LODGING',
      locality: 'Tokyo',
      tier: 'boutique',
      partySize: 2,
      preferredCurrency: 'USD'
    };
    const result = await fetchAmadeusQuote(request);
    expect(result.status).toBe('UNAVAILABLE');
    if (result.status === 'UNAVAILABLE') {
      expect(result.reason).toContain('AMADEUS_API_KEY/AMADEUS_API_SECRET missing');
    }
  });

  test('Amadeus supplier returns UNAVAILABLE for flights without origin', async () => {
    const request: QuoteRequest = {
      category: 'FLIGHT',
      locality: 'Tokyo'
    };
    const result = await fetchAmadeusQuote(request);
    expect(result.status).toBe('UNAVAILABLE');
    if (result.status === 'UNAVAILABLE') {
      expect(result.reason).toContain('without an origin');
    }
  });

  test('Google Places supplier returns UNAVAILABLE when API key is missing', async () => {
    const request: QuoteRequest = {
      category: 'DINING',
      locality: 'Rome',
      tier: 'fine_dining',
      partySize: 2
    };
    const result = await fetchPlacesQuote(request);
    expect(result.status).toBe('UNAVAILABLE');
    if (result.status === 'UNAVAILABLE') {
      expect(result.reason).toContain('GOOGLE_PLACES_API_KEY missing');
    }
  });

  test('FX supplier returns UNAVAILABLE when API key is missing', async () => {
    const request: QuoteRequest = {
      category: 'FX',
      tier: 'USD',
      preferredCurrency: 'EUR'
    };
    const result = await fetchFxQuote(request);
    expect(result.status).toBe('UNAVAILABLE');
    if (result.status === 'UNAVAILABLE') {
      expect(result.reason).toContain('FX_API_KEY missing');
    }
  });
});
