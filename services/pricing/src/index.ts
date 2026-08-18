import express, { Request, Response } from 'express';
import cors from 'cors';
import { QuoteRequest, QuoteResult } from './types';
import { fetchFxQuote } from './suppliers/fxSupplier';
import { fetchPlacesQuote } from './suppliers/placesSupplier';
import { fetchAmadeusQuote } from './suppliers/amadeusSupplier';

const app = express();
const PORT = process.env.PORT || 8085;

app.use(cors());
app.use(express.json());

async function routeQuote(request: QuoteRequest): Promise<QuoteResult> {
  const category = (request.category || '').toUpperCase();

  if (category === 'FX' || category === 'CURRENCY') {
    return fetchFxQuote(request);
  }

  if (category === 'DINING' || category === 'ACTIVITY') {
    return fetchPlacesQuote(request);
  }

  if (category === 'FLIGHT' || category === 'TRANSPORT' || category === 'LODGING') {
    return fetchAmadeusQuote(request);
  }

  return {
    status: 'UNAVAILABLE',
    reason: `Unsupported pricing category: ${request.category}`
  };
}

app.post('/quote', async (req: Request, res: Response) => {
  try {
    const request: QuoteRequest = req.body;
    const result = await routeQuote(request);
    res.json(result);
  } catch (err: any) {
    res.status(500).json({
      status: 'UNAVAILABLE',
      reason: err.message || 'Internal pricing service error'
    });
  }
});

app.post('/quotes', async (req: Request, res: Response) => {
  try {
    const requests: QuoteRequest[] = req.body.requests || [];
    const results = await Promise.all(requests.map(routeQuote));
    res.json({ results });
  } catch (err: any) {
    res.status(500).json({
      status: 'UNAVAILABLE',
      reason: err.message || 'Internal pricing service error'
    });
  }
});

app.get('/health', (_req: Request, res: Response) => {
  res.json({ status: 'OK', timestamp: Date.now() });
});

if (require.main === module) {
  app.listen(PORT, () => {
    console.log(`Marco Pricing Service listening on port ${PORT}`);
  });
}

export default app;
