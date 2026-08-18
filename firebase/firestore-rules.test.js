const fs = require('fs');
const path = require('path');
const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds
} = require('@firebase/rules-unit-testing');
const { doc, getDoc, setDoc, updateDoc } = require('firebase/firestore');

const PROJECT_ID = 'go-marco';

describe('Firestore Security Rules — Privacy & Hybrid Model', () => {
  let testEnv;

  beforeAll(async () => {
    const rulesPath = path.resolve(__dirname, '../firestore.rules');
    const rules = fs.readFileSync(rulesPath, 'utf8');

    testEnv = await initializeTestEnvironment({
      projectId: PROJECT_ID,
      firestore: {
        rules: rules,
        host: '127.0.0.1',
        port: 8080
      }
    });
  });

  afterAll(async () => {
    if (testEnv) {
      await testEnv.cleanup();
    }
  });

  beforeEach(async () => {
    await testEnv.clearFirestore();

    // Seed a standard trip with Alice (ORGANIZER), Bob (TRAVELER), and Charlie (VIEWER)
    await testEnv.withSecurityRulesDisabled(async (context) => {
      const db = context.firestore();
      await setDoc(doc(db, 'trips', 'trip-123'), {
        title: 'Alpine Expedition',
        members: {
          alice: { role: 'ORGANIZER', travelerId: 1, state: 'ACTIVE' },
          bob: { role: 'TRAVELER', travelerId: 2, state: 'ACTIVE' },
          charlie: { role: 'VIEWER', travelerId: 3, state: 'ACTIVE' }
        }
      });
      await setDoc(doc(db, 'users', 'alice', 'profile', 'info'), {
        name: 'Alice',
        privateData: 'secret-123'
      });
    });
  });

  // 1. A member cannot read another member's /users/{uid} data. Nothing under /users is ever shared.
  test('1. Member Bob cannot read Alice private /users/alice data', async () => {
    const bobContext = testEnv.authenticatedContext('bob');
    const bobDb = bobContext.firestore();
    const aliceDocRef = doc(bobDb, 'users', 'alice', 'profile', 'info');
    await assertFails(getDoc(aliceDocRef));

    const aliceContext = testEnv.authenticatedContext('alice');
    const aliceDb = aliceContext.firestore();
    await assertSucceeds(getDoc(doc(aliceDb, 'users', 'alice', 'profile', 'info')));
  });

  // 2. A non-member cannot read /trips/{tripId} or any subcollection.
  test('2. Non-member Eve cannot read /trips/trip-123 or its subcollections', async () => {
    const eveContext = testEnv.authenticatedContext('eve');
    const eveDb = eveContext.firestore();
    await assertFails(getDoc(doc(eveDb, 'trips', 'trip-123')));
    await assertFails(getDoc(doc(eveDb, 'trips', 'trip-123', 'partyUnits', 'unit-1')));
    await assertFails(getDoc(doc(eveDb, 'trips', 'trip-123', 'briefs', 'brief-1')));
  });

  // 3. A VIEWER can read but cannot write any shared collection.
  test('3. VIEWER Charlie can read trip and subcollections, but cannot create or write', async () => {
    const charlieContext = testEnv.authenticatedContext('charlie');
    const charlieDb = charlieContext.firestore();

    // Read succeeds
    await assertSucceeds(getDoc(doc(charlieDb, 'trips', 'trip-123')));

    // Write fails
    await assertFails(
      setDoc(doc(charlieDb, 'trips', 'trip-123', 'itinerary', 'item-1'), {
        title: 'Morning Hike',
        category: 'ACTIVITY'
      })
    );
  });

  // 4. A TRAVELER cannot modify the members map; an ORGANIZER can. (Privilege escalation test).
  test('4. TRAVELER Bob cannot modify members map (privilege escalation), ORGANIZER Alice can', async () => {
    const bobContext = testEnv.authenticatedContext('bob');
    const bobDb = bobContext.firestore();

    // Bob trying to promote himself to ORGANIZER
    await assertFails(
      updateDoc(doc(bobDb, 'trips', 'trip-123'), {
        members: {
          alice: { role: 'ORGANIZER', travelerId: 1, state: 'ACTIVE' },
          bob: { role: 'ORGANIZER', travelerId: 2, state: 'ACTIVE' },
          charlie: { role: 'VIEWER', travelerId: 3, state: 'ACTIVE' }
        }
      })
    );

    // Alice modifying members map
    const aliceContext = testEnv.authenticatedContext('alice');
    const aliceDb = aliceContext.firestore();
    await assertSucceeds(
      updateDoc(doc(aliceDb, 'trips', 'trip-123'), {
        members: {
          alice: { role: 'ORGANIZER', travelerId: 1, state: 'ACTIVE' },
          bob: { role: 'ORGANIZER', travelerId: 2, state: 'ACTIVE' },
          charlie: { role: 'VIEWER', travelerId: 3, state: 'ACTIVE' }
        }
      })
    );
  });

  // 5. Writing { ownerUid, programTitle, programType } to /trips/{t}/memberPrograms/{p} succeeds.
  test('5. Writing clean program ref { ownerUid, programTitle, programType } succeeds', async () => {
    const bobContext = testEnv.authenticatedContext('bob');
    const bobDb = bobContext.firestore();

    await assertSucceeds(
      setDoc(doc(bobDb, 'trips', 'trip-123', 'memberPrograms', 'prog-bonvoy'), {
        ownerUid: 'bob',
        programTitle: 'Marriott Bonvoy',
        programType: 'HOTEL',
        addedAtMillis: 1700000000000
      })
    );
  });

  // 6. The same write plus balanceValue, rewardsEstimatedValuationUsd, tierStatus, or accountNumberMasked is rejected.
  test('6. Writing memberProgram with balanceValue, valuation, tier, or accountNumber is rejected', async () => {
    const bobContext = testEnv.authenticatedContext('bob');
    const bobDb = bobContext.firestore();

    // Leaking balanceValue
    await assertFails(
      setDoc(doc(bobDb, 'trips', 'trip-123', 'memberPrograms', 'prog-bonvoy-leak1'), {
        ownerUid: 'bob',
        programTitle: 'Marriott Bonvoy',
        programType: 'HOTEL',
        balanceValue: '120,000'
      })
    );

    // Leaking rewardsEstimatedValuationUsd
    await assertFails(
      setDoc(doc(bobDb, 'trips', 'trip-123', 'memberPrograms', 'prog-bonvoy-leak2'), {
        ownerUid: 'bob',
        programTitle: 'Marriott Bonvoy',
        programType: 'HOTEL',
        rewardsEstimatedValuationUsd: 600.0
      })
    );

    // Leaking tierStatus
    await assertFails(
      setDoc(doc(bobDb, 'trips', 'trip-123', 'memberPrograms', 'prog-bonvoy-leak3'), {
        ownerUid: 'bob',
        programTitle: 'Marriott Bonvoy',
        programType: 'HOTEL',
        tierStatus: 'Platinum'
      })
    );

    // Leaking accountNumberMasked
    await assertFails(
      setDoc(doc(bobDb, 'trips', 'trip-123', 'memberPrograms', 'prog-bonvoy-leak4'), {
        ownerUid: 'bob',
        programTitle: 'Marriott Bonvoy',
        programType: 'HOTEL',
        accountNumberMasked: '•••• 4432'
      })
    );
  });

  // 7. A contribution with agreedValueAmount but no agreedByUids / agreedAtMillis is rejected — an agreement must be signed.
  test('7. Contribution with unsigned agreedValueAmount is rejected; signed is accepted', async () => {
    const bobContext = testEnv.authenticatedContext('bob');
    const bobDb = bobContext.firestore();

    // Unsigned agreement (missing agreedByUids and agreedAtMillis)
    await assertFails(
      setDoc(doc(bobDb, 'trips', 'trip-123', 'contributions', 'contrib-1'), {
        contributorUid: 'bob',
        assetKind: 'TIMESHARE_WEEK',
        state: 'AGREED',
        agreedValueAmount: 2400.0,
        agreedValueCurrency: 'USD'
      })
    );

    // Properly signed agreement
    await assertSucceeds(
      setDoc(doc(bobDb, 'trips', 'trip-123', 'contributions', 'contrib-1'), {
        contributorUid: 'bob',
        assetKind: 'TIMESHARE_WEEK',
        state: 'AGREED',
        agreedValueAmount: 2400.0,
        agreedValueCurrency: 'USD',
        agreedAtMillis: 1700000000000,
        agreedByUids: ['alice', 'bob']
      })
    );
  });

  // 8. A contribution with proposedValueAmount but no recognized proposalSource is rejected — a proposal must be labelled.
  test('8. Contribution with unlabelled proposal is rejected; labelled proposal is accepted', async () => {
    const bobContext = testEnv.authenticatedContext('bob');
    const bobDb = bobContext.firestore();

    // Unlabelled proposal
    await assertFails(
      setDoc(doc(bobDb, 'trips', 'trip-123', 'contributions', 'contrib-2'), {
        contributorUid: 'bob',
        assetKind: 'POINTS',
        state: 'OFFERED',
        proposedValueAmount: 500.0,
        proposedValueCurrency: 'USD'
      })
    );

    // Invalid proposal source
    await assertFails(
      setDoc(doc(bobDb, 'trips', 'trip-123', 'contributions', 'contrib-2'), {
        contributorUid: 'bob',
        assetKind: 'POINTS',
        state: 'OFFERED',
        proposedValueAmount: 500.0,
        proposedValueCurrency: 'USD',
        proposalSource: 'UNKNOWN_RANDOM_SOURCE'
      })
    );

    // Labelled proposal with valid source
    await assertSucceeds(
      setDoc(doc(bobDb, 'trips', 'trip-123', 'contributions', 'contrib-2'), {
        contributorUid: 'bob',
        assetKind: 'POINTS',
        state: 'OFFERED',
        proposedValueAmount: 500.0,
        proposedValueCurrency: 'USD',
        proposalSource: 'MODELED'
      })
    );
  });

  // 9. A nested map smuggled inside an allowed key (e.g. programTitle: { balance: 5000 }) is rejected by the scalar type check.
  test('9. Smuggled nested map inside allowed scalar key is rejected', async () => {
    const bobContext = testEnv.authenticatedContext('bob');
    const bobDb = bobContext.firestore();

    await assertFails(
      setDoc(doc(bobDb, 'trips', 'trip-123', 'memberPrograms', 'prog-smuggled'), {
        ownerUid: 'bob',
        programTitle: { balance: 50000, name: 'Marriott' },
        programType: 'HOTEL'
      })
    );
  });

  // 10. Writing to /trips/{t}/powWowTranscripts/{x} is rejected for every role.
  test('10. Writing to /trips/{t}/powWowTranscripts/{x} is rejected for ORGANIZER and TRAVELER', async () => {
    const aliceContext = testEnv.authenticatedContext('alice');
    const aliceDb = aliceContext.firestore();
    const bobContext = testEnv.authenticatedContext('bob');
    const bobDb = bobContext.firestore();

    await assertFails(
      setDoc(doc(aliceDb, 'trips', 'trip-123', 'powWowTranscripts', 'tx-1'), {
        transcriptText: 'Raw private dump from Alice'
      })
    );

    await assertFails(
      setDoc(doc(bobDb, 'trips', 'trip-123', 'powWowTranscripts', 'tx-2'), {
        transcriptText: 'Raw private dump from Bob'
      })
    );
  });
});
