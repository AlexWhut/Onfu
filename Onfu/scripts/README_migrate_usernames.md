Migration script: migrate_usernames.js

Purpose
- Populate a minimal public collection `usernames/{userid}` from existing `users` documents.
- Each `usernames/{userid}` will contain `{ uid: <user uid>, email: <email?> }`.

Why
- This makes username → email lookup a single-document read (GET), avoiding Firestore rule issues with unauthenticated clients running queries on `/users`.

Prerequisites
- Node.js installed (v14+ recommended).
- A Firebase service account JSON (Admin SDK) for the target project. Download from Firebase Console > Project Settings > Service accounts.

Install
1) Open a terminal in this repo and run:

```bash
cd scripts
npm install firebase-admin
```

Run (dry run)

```bash
node migrate_usernames.js --serviceKey=./serviceAccountKey.json --dryRun
```

Run (apply changes)

```bash
node migrate_usernames.js --serviceKey=./serviceAccountKey.json
```

Optional
- `--limit=N` to process only N user documents (helpful for testing).

Safety
- The script warns about conflicts when a `usernames/{userid}` exists with a different `uid`.
- It does not delete or modify existing conflicting docs; review conflicts before acting.

Afterwards
- Confirm that `usernames` documents exist in Firestore.
- Update rules if needed (you can make `usernames` readable by anyone while keeping `users` protected).

Example Firestore rules snippet for `usernames`:

```js
match /usernames/{userid} {
  allow read: if true; // public read for username lookup
  allow create: if request.auth != null
                && request.resource.data.uid == request.auth.uid
                && request.resource.data.uid is string
                && request.resource.data.email is string;
  allow update: if false;
  allow delete: if request.auth != null && resource.data.uid == request.auth.uid;
}
```

If you want, I can also add a Gradle task or a small Java/Kotlin CLI instead of Node.js.