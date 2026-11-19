const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
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

test("Unauthenticated user cannot list or write calendarEvents", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("calendarEvents").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("calendarEvents").doc("evt_1").set({
      userId: ALICE_UID,
      title: "Reunión",
      dateKey: "2026-11-18",
      timeLabel: "18:00",
      category: "TRABAJO",
      note: "",
      completed: false,
      colorHex: "#00FFA3",
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Owner can create, list, toggle completed, and delete calendarEvents matching Kotlin payload", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 5000);
  const colRef = aliceDb.collection("users").doc(ALICE_UID).collection("calendarEvents");

  await assertSucceeds(
    colRef.doc("evt_1").set({
      userId: ALICE_UID,
      title: "Revisión de Despliegue",
      dateKey: "2026-11-20",
      timeLabel: "19:30",
      category: "OPERACIONES",
      note: "Ventana de mantenimiento",
      completed: false,
      colorHex: "#00E5FF",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(colRef.get());

  await assertSucceeds(
    colRef.doc("evt_1").update({
      completed: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject mutating createdAt or userId
  await assertFails(
    colRef.doc("evt_1").update({
      createdAt: new Date(Date.now() - 200),
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // Reject unexpected @DocumentId 'id' field in payload
  await assertFails(
    colRef.doc("evt_2").set({
      id: "evt_2",
      userId: ALICE_UID,
      title: "Invalid ID field",
      dateKey: "2026-11-20",
      timeLabel: "",
      category: "PERSONAL",
      note: "",
      completed: false,
      colorHex: "#00FFA3",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(colRef.doc("evt_1").delete());
});

test("Cross-user isolation blocks Bob from Alice's calendarEvents", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(ALICE_UID).collection("calendarEvents").doc("evt_a").set({
      userId: ALICE_UID,
      title: "Privado",
      dateKey: "2026-11-18",
      timeLabel: "10:00",
      category: "PERSONAL",
      note: "",
      completed: false,
      colorHex: "#00FFA3",
      createdAt: new Date(Date.now() - 5000),
      updatedAt: new Date(Date.now() - 5000),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("calendarEvents").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("calendarEvents").doc("evt_a").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("calendarEvents").doc("evt_a").update({
      completed: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});
