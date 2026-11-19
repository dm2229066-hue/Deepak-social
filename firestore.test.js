const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_1";
const BOB_UID = "bob_uid_2";
const ADMIN_UID = "admin_uid_0";
const ADMIN_EMAIL = "rahul11223344d@gmail.com";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(msOffset = 2000) {
  return new Date(Date.now() - msOffset);
}

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

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauth.collection("public_profiles").where("accountStatus", "==", "active").get());
  await assertFails(unauth.collection("posts").where("visibility", "==", "public").get());
  await assertFails(unauth.collection("users").doc(ALICE_UID).get());
  await assertFails(unauth.collection("wallets").doc(ALICE_UID).get());
});

test("PII Isolation: User can read/write own private user doc, cannot read another user's PII", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true }).firestore();
  const now = pastDate();

  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).set({
      uid: ALICE_UID,
      email: "alice@example.com",
      privacyMode: "public",
      blockedUserIds: [],
      termsAccepted: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(alice.collection("users").doc(ALICE_UID).get());
  await assertFails(bob.collection("users").doc(ALICE_UID).get());
  await assertFails(bob.collection("users").doc(ALICE_UID).update({ upiId: "hacker@upi", updatedAt: pastDate(500) }));
});

test("Public profiles: Owner create/update succeeds; cannot self-approve monetization or inject @DocumentId id", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = pastDate();

  await assertSucceeds(
    alice.collection("public_profiles").doc(ALICE_UID).set({
      uid: ALICE_UID,
      username: "alice_creator",
      displayName: "Alice",
      bio: "Creator on Deepak Social",
      followersCount: 0,
      followingCount: 0,
      friendsCount: 0,
      postsCount: 0,
      totalViews: 0,
      monetizationStatus: "none",
      referralCode: "DS-ALICE1",
      online: true,
      lastSeenAt: now,
      accountStatus: "active",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Valid partial update
  await assertSucceeds(
    alice.collection("public_profiles").doc(ALICE_UID).update({
      bio: "Updated creator bio",
      updatedAt: pastDate(500),
    })
  );

  // Self-approving monetization must fail
  await assertFails(
    alice.collection("public_profiles").doc(ALICE_UID).update({
      monetizationStatus: "approved",
      updatedAt: pastDate(500),
    })
  );

  // Injecting @DocumentId 'id' field must fail
  await assertFails(
    alice.collection("public_profiles").doc(BOB_UID).set({
      id: BOB_UID,
      uid: BOB_UID,
      username: "bob",
      displayName: "Bob",
      bio: "",
      followersCount: 0,
      followingCount: 0,
      friendsCount: 0,
      postsCount: 0,
      totalViews: 0,
      monetizationStatus: "none",
      referralCode: "DS-BOB1",
      online: true,
      lastSeenAt: now,
      accountStatus: "active",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Filtered query succeeds, unfiltered query fails
  await assertSucceeds(
    alice.collection("public_profiles").where("accountStatus", "==", "active").get()
  );
  await assertFails(alice.collection("public_profiles").get());
});

test("Posts & Comments: Kotlin-aligned payload, query filters, and ownership", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = pastDate();

  await assertSucceeds(
    alice.collection("posts").doc("post_1").set({
      authorId: ALICE_UID,
      authorName: "Alice",
      authorUsername: "alice_creator",
      text: "Hello #DeepakSocial!",
      mediaType: "none",
      hashtags: ["deepaksocial"],
      visibility: "public",
      likesCount: 0,
      commentsCount: 0,
      sharesCount: 0,
      viewsCount: 0,
      reelPost: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Unfiltered list on posts fails; filtered public list succeeds
  await assertFails(bob.collection("posts").get());
  await assertSucceeds(bob.collection("posts").where("visibility", "==", "public").get());

  // Bob can comment on Alice's public post
  await assertSucceeds(
    bob.collection("posts").doc("post_1").collection("comments").doc("c_1").set({
      postId: "post_1",
      authorId: BOB_UID,
      authorName: "Bob",
      text: "Awesome post!",
      postVisibility: "public",
      createdAt: pastDate(500),
    })
  );

  // Bob cannot delete Alice's post
  await assertFails(bob.collection("posts").doc("post_1").delete());
});

test("Wallet & Withdrawal Security: Users cannot inflate balances or withdraw above balance", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = pastDate();

  // Initializing non-zero wallet as user must fail
  await assertFails(
    alice.collection("wallets").doc(ALICE_UID).set({
      uid: ALICE_UID,
      totalEarnings: 9999,
      dailyEarnings: 0,
      weeklyEarnings: 0,
      monthlyEarnings: 0,
      lifetimeEarnings: 9999,
      adRevenue: 0,
      videoEarnings: 0,
      postEarnings: 0,
      referralEarnings: 0,
      availableBalance: 9999,
      pendingBalance: 0,
      updatedAt: now,
    })
  );

  // Zero wallet initialization succeeds
  await assertSucceeds(
    alice.collection("wallets").doc(ALICE_UID).set({
      uid: ALICE_UID,
      totalEarnings: 0,
      dailyEarnings: 0,
      weeklyEarnings: 0,
      monthlyEarnings: 0,
      lifetimeEarnings: 0,
      adRevenue: 0,
      videoEarnings: 0,
      postEarnings: 0,
      referralEarnings: 0,
      availableBalance: 0,
      pendingBalance: 0,
      updatedAt: now,
    })
  );

  // User cannot update own wallet balance
  await assertFails(
    alice.collection("wallets").doc(ALICE_UID).update({
      availableBalance: 500,
      updatedAt: pastDate(500),
    })
  );

  // Withdrawal above 0 availableBalance must fail
  await assertFails(
    alice.collection("withdrawal_requests").doc("wd_1").set({
      userId: ALICE_UID,
      userName: "Alice",
      amount: 100,
      method: "upi",
      destination: "alice@upi",
      status: "pending",
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Admin RBAC & Email Spoofing Protection", async () => {
  const spoofedAdmin = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: false,
  }).firestore();

  const verifiedAdmin = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();

  const now = pastDate();

  // Spoofed unverified email cannot write app_config or announcements
  await assertFails(
    spoofedAdmin.collection("app_config").doc("rewards").set({
      referralRewardInr: 50,
      minWithdrawalInr: 100,
      minFriendsForMonetization: 5,
      minPostsForMonetization: 3,
      minViewsForMonetization: 100,
      updatedAt: now,
    })
  );

  // Verified admin succeeds
  await assertSucceeds(
    verifiedAdmin.collection("app_config").doc("rewards").set({
      referralRewardInr: 50,
      minWithdrawalInr: 100,
      minFriendsForMonetization: 5,
      minPostsForMonetization: 3,
      minViewsForMonetization: 100,
      updatedAt: now,
    })
  );

  // Verified admin can credit a creator wallet
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("wallets").doc(ALICE_UID).set({
      uid: ALICE_UID,
      totalEarnings: 0,
      dailyEarnings: 0,
      weeklyEarnings: 0,
      monthlyEarnings: 0,
      lifetimeEarnings: 0,
      adRevenue: 0,
      videoEarnings: 0,
      postEarnings: 0,
      referralEarnings: 0,
      availableBalance: 0,
      pendingBalance: 0,
      updatedAt: now,
    });
  });

  await assertSucceeds(
    verifiedAdmin.collection("wallets").doc(ALICE_UID).update({
      totalEarnings: 250,
      dailyEarnings: 250,
      weeklyEarnings: 250,
      monthlyEarnings: 250,
      lifetimeEarnings: 250,
      adRevenue: 250,
      availableBalance: 250,
      updatedAt: pastDate(500),
    })
  );
});

test("1-to-1 Chat isolation & Referral anti-fraud", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();
  const intruder = testEnv.authenticatedContext("intruder_99").firestore();
  const now = pastDate();
  const chatId = `${ALICE_UID}_${BOB_UID}`;

  await assertSucceeds(
    alice.collection("chats").doc(chatId).set({
      participantUids: [ALICE_UID, BOB_UID],
      user1Uid: ALICE_UID,
      user1Name: "Alice",
      user2Uid: BOB_UID,
      user2Name: "Bob",
      lastMessage: "Hi Bob!",
      lastSenderId: ALICE_UID,
      lastMessageAt: now,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    bob.collection("chats").doc(chatId).collection("messages").doc("m_1").set({
      chatId,
      senderId: BOB_UID,
      participantUids: [ALICE_UID, BOB_UID],
      text: "Hey Alice!",
      mediaType: "none",
      createdAt: pastDate(500),
    })
  );

  // Intruder cannot read chat, fetch message doc, run unfiltered list, or query Alice's messages
  await assertFails(intruder.collection("chats").doc(chatId).get());
  await assertFails(intruder.collection("chats").doc(chatId).collection("messages").doc("m_1").get());
  await assertFails(intruder.collection("chats").doc(chatId).collection("messages").get());
  await assertFails(
    intruder.collection("chats").doc(chatId).collection("messages").where("participantUids", "array-contains", ALICE_UID).get()
  );

  // Self-referral must fail
  await assertFails(
    alice.collection("referrals").doc(ALICE_UID).set({
      referrerUid: ALICE_UID,
      referredUid: ALICE_UID,
      referredName: "Alice",
      referralCode: "DS-ALICE1",
      rewardAmount: 0,
      status: "pending",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Valid referral by Bob using Alice's code succeeds with 0 initial rewardAmount
  await assertSucceeds(
    bob.collection("referrals").doc(BOB_UID).set({
      referrerUid: ALICE_UID,
      referredUid: BOB_UID,
      referredName: "Bob",
      referralCode: "DS-ALICE1",
      rewardAmount: 0,
      status: "pending",
      createdAt: now,
      updatedAt: now,
    })
  );
});
