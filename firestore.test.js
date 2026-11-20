const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require('@firebase/rules-unit-testing');
const { test, before, after, beforeEach } = require('node:test');
const fs = require('node:fs');

const PROJECT_ID = process.env.GCP_PROJECT || 'demo-no-project';
const ALICE_UID = 'alice_123';
const BOB_UID = 'bob_456';

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || '127.0.0.1:8085'
).split(':');
const emulatorPort = parseInt(emulatorPortStr, 10);

let testEnv;

function validSummary(uid, ts = new Date(Date.now() - 2000)) {
  return {
    userId: uid,
    ownerEmail: 'alice@example.com',
    transactionCount: 2,
    categoryCount: 1,
    accountCount: 1,
    defaultCurrency: 'EGP',
    language: 'ar',
    themeMode: 'light',
    quickDeleteMode: true,
    dailyReminderEnabled: true,
    dailyReminderTime: '21:00',
    monthlyBudgetLimitMinor: 500000,
    lastSelectedSource: 'كاش / نقدي',
    userName: 'محمد',
    createdAt: ts,
    updatedAt: ts,
  };
}

function validTransaction(uid, ts = new Date(Date.now() - 2000)) {
  return {
    userId: uid,
    type: 'expense',
    amountMinor: 12500,
    currency: 'EGP',
    source: 'كاش / نقدي',
    category: 'طعام ومشروبات',
    date: '2026-11-20',
    time: '14:30',
    note: 'غداء',
    localCreatedAt: 1795100000000,
    debt: false,
    debtSettled: false,
    itemName: '',
    debtParty: '',
    createdAt: ts,
    updatedAt: ts,
  };
}

function validCategory(uid, ts = new Date(Date.now() - 2000)) {
  return {
    userId: uid,
    name: 'طعام ومشروبات',
    iconKey: 'restaurant',
    kind: 'expense',
    createdAt: ts,
    updatedAt: ts,
  };
}

function validAccount(uid, ts = new Date(Date.now() - 2000)) {
  return {
    userId: uid,
    name: 'كاش / نقدي',
    iconKey: 'wallet',
    currency: 'EGP',
    createdAt: ts,
    updatedAt: ts,
  };
}

before(async () => {
  const rules = fs.readFileSync('./firestore.rules', 'utf8');
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test('Unauthenticated users cannot read or write any cloud backup path', async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.doc(`users/${ALICE_UID}/backups/latest`).get());
  await assertFails(
    unauthDb.doc(`users/${ALICE_UID}/backups/latest`).set(validSummary(ALICE_UID))
  );
  await assertFails(
    unauthDb.collection(`users/${ALICE_UID}/backup_transactions`).get()
  );
  await assertFails(
    unauthDb
      .doc(`users/${ALICE_UID}/backup_transactions/tx_1`)
      .set(validTransaction(ALICE_UID))
  );
  await assertFails(
    unauthDb
      .doc(`users/${ALICE_UID}/backup_categories/cat_1`)
      .set(validCategory(ALICE_UID))
  );
  await assertFails(
    unauthDb
      .doc(`users/${ALICE_UID}/backup_accounts/acc_1`)
      .set(validAccount(ALICE_UID))
  );
  await assertFails(unauthDb.doc('unknown_collection/doc_1').get());
});

test('Authenticated owner can create, read, list, update, and delete their own backup records', async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  // Create
  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/backups/latest`).set(validSummary(ALICE_UID, created))
  );
  await assertSucceeds(
    aliceDb
      .doc(`users/${ALICE_UID}/backup_transactions/tx_1`)
      .set(validTransaction(ALICE_UID, created))
  );
  await assertSucceeds(
    aliceDb
      .doc(`users/${ALICE_UID}/backup_categories/cat_1`)
      .set(validCategory(ALICE_UID, created))
  );
  await assertSucceeds(
    aliceDb
      .doc(`users/${ALICE_UID}/backup_accounts/acc_1`)
      .set(validAccount(ALICE_UID, created))
  );

  // Read & List (including empty/populated subcollections)
  await assertSucceeds(aliceDb.doc(`users/${ALICE_UID}/backups/latest`).get());
  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/backup_transactions`).get()
  );
  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/backup_categories`).get()
  );
  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/backup_accounts`).get()
  );

  // Partial Update (preserving userId & createdAt)
  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/backups/latest`).update({
      transactionCount: 5,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/backup_transactions/tx_1`).update({
      debt: true,
      debtSettled: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Delete
  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/backup_transactions/tx_1`).delete()
  );
  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/backup_categories/cat_1`).delete()
  );
  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/backup_accounts/acc_1`).delete()
  );
  await assertSucceeds(aliceDb.doc(`users/${ALICE_UID}/backups/latest`).delete());
});

test('Strict cross-user isolation blocks Bob from accessing or modifying Alice backups', async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/backups/latest`).set(validSummary(ALICE_UID))
  );
  await assertSucceeds(
    aliceDb
      .doc(`users/${ALICE_UID}/backup_transactions/tx_1`)
      .set(validTransaction(ALICE_UID))
  );

  await assertFails(bobDb.doc(`users/${ALICE_UID}/backups/latest`).get());
  await assertFails(
    bobDb.collection(`users/${ALICE_UID}/backup_transactions`).get()
  );
  await assertFails(
    bobDb
      .doc(`users/${ALICE_UID}/backup_transactions/tx_2`)
      .set(validTransaction(BOB_UID))
  );
  await assertFails(
    bobDb.doc(`users/${ALICE_UID}/backups/latest`).update({
      transactionCount: 99,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    bobDb.doc(`users/${ALICE_UID}/backup_transactions/tx_1`).delete()
  );
});

test('Rejects ghost fields, @DocumentId id key, invalid enums, oversized strings, future timestamps, and immutable tampering', async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  // Ghost field & forbidden id field
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/backups/latest`).set({
      ...validSummary(ALICE_UID),
      isAdmin: true,
    })
  );
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/backup_transactions/tx_with_id`).set({
      id: 'tx_with_id',
      ...validTransaction(ALICE_UID),
    })
  );

  // Invalid enum
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/backup_transactions/tx_bad_type`).set({
      ...validTransaction(ALICE_UID),
      type: 'transfer',
    })
  );

  // Oversized note (> 500 chars)
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/backup_transactions/tx_long_note`).set({
      ...validTransaction(ALICE_UID),
      note: 'a'.repeat(501),
    })
  );

  // Future timestamp spoofing
  const futureTs = new Date(Date.now() + 60000);
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/backup_accounts/acc_future`).set({
      ...validAccount(ALICE_UID),
      createdAt: futureTs,
    })
  );

  // Immutable userId / createdAt tampering on update
  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/backups/latest`).set(validSummary(ALICE_UID))
  );
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/backups/latest`).update({
      userId: BOB_UID,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/backups/latest`).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );
});
