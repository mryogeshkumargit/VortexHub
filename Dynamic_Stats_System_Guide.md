# Guide: Implementing Dynamic Stats (Score, XP, Affection) in AI Character Systems

This guide explains how to design and implement a **dynamic stats
system** (e.g., Score, XP, Affection, Relationship Levels) in an AI
character app.\
It is written for developers (e.g., using Cursor AI, SillyTavern-like
apps, or custom bots).

------------------------------------------------------------------------

## 🔹 1. Concept Overview

Dynamic stats allow an AI character to **track and update variables**
during conversation.\
For example: - **Score**: General points based on user actions.\
- **XP (Experience Points)**: Earned when completing actions or
progressing the story.\
- **Affection**: Relationship level that increases or decreases based on
dialogue.

These values: - Start with **initial defaults**.\
- Update **dynamically** depending on user messages.\
- Get **displayed after each AI response** for immersion.

------------------------------------------------------------------------

## 🔹 2. Core Components of the System

1.  **Variable Definitions**
    -   Stored in memory or state object.\

    -   Example:

        ``` json
        {
          "score": 0,
          "xp": 0,
          "affection": 5
        }
        ```
2.  **Trigger Rules**
    -   Define how messages affect stats.\

    -   Triggers can be **regex patterns** or **keyword matches**.\

    -   Example rule:

        ``` json
        {
          "trigger": "give.*flower",
          "action": "affection+2; xp+1"
        }
        ```
3.  **Update Mechanism**
    -   Parse the user/AI message.\
    -   Match triggers.\
    -   Apply corresponding stat updates.
4.  **Display Layer**
    -   Append stats after each AI response.\

    -   Example output:

            Aisha blushes and smiles.
            ---
            XP: 12 | Affection: 7 | Score: 5

------------------------------------------------------------------------

## 🔹 3. Workflow

1.  **Initialize Stats**
    -   Create default values when a new chat session starts.
2.  **On Each Message**
    -   Capture last user and AI message.\
    -   Run **trigger checks**.\
    -   If a trigger matches, update the stat.
3.  **After AI Response**
    -   Append current stats to the message block.\
    -   Ensure formatting (e.g., a separator line `---`).

------------------------------------------------------------------------

## 🔹 4. Technical Implementation

### A. Data Structure

``` js
// Example: Session state object
let stats = {
  score: 0,
  xp: 0,
  affection: 5
};
```

### B. Trigger Matching

``` js
// Example triggers
const triggers = [
  { pattern: /thank/i, action: () => stats.affection++ },
  { pattern: /fight/i, action: () => { stats.xp += 5; stats.score += 2; } },
  { pattern: /insult/i, action: () => stats.affection -= 2 }
];
```

### C. Update Loop

``` js
function processMessage(message) {
  for (const t of triggers) {
    if (t.pattern.test(message)) {
      t.action();
    }
  }
}
```

### D. Appending Stats to AI Output

``` js
function appendStats(aiResponse) {
  return `${aiResponse}
---
XP: ${stats.xp} | Affection: ${stats.affection} | Score: ${stats.score}`;
}
```

### E. Example Conversation

``` txt
User: I give you flowers.
AI: Aisha blushes and thanks you.
---
XP: 1 | Affection: 6 | Score: 0

User: I insult you.
AI: Aisha frowns and looks away.
---
XP: 1 | Affection: 4 | Score: 0
```

------------------------------------------------------------------------

## 🔹 5. Integration Into Existing App

1.  **Identify Message Pipeline**
    -   Where user input and AI output are processed.\
    -   Hook into this pipeline to **process triggers**.
2.  **Store Stats Persistently**
    -   Keep stats in memory during session.\
    -   Optionally save in **localStorage / database** for persistence.
3.  **Custom Display**
    -   Format stats as desired (inline badges, HUD-style overlay,
        markdown table, etc.).
4.  **Optional: Dynamic Influence**
    -   Stats can **feed back into AI prompt**.\

    -   Example system prompt injection:

            Current Stats: XP {{xp}}, Affection {{affection}}, Score {{score}}.

    -   This way, the AI **knows about current stats** and can roleplay
        accordingly.

------------------------------------------------------------------------

## 🔹 6. Advanced Extensions

-   **Leveling System**

    ``` js
    let level = Math.floor(stats.xp / 10);
    ```

-   **Conditional Responses**

    -   AI behavior changes if `affection > 10`.\

    -   Add conditional text in character card:

            {{#if affection>10}} Aisha is very fond of {{user}}. {{/if}}

-   **Multiple Characters**

    -   Track per-character stats using nested objects.

-   **UI Widgets**

    -   Display progress bars or meters for stats.

------------------------------------------------------------------------

## 🔹 7. Example Pseudo-Code

``` js
// Conversation loop example
function handleConversation(userMsg, aiResponse) {
  // Step 1: Process triggers
  processMessage(userMsg);
  processMessage(aiResponse);

  // Step 2: Append stats to AI response
  let finalOutput = appendStats(aiResponse);

  return finalOutput;
}
```

------------------------------------------------------------------------

## 🔹 8. Summary

-   Define **stat variables** (XP, score, affection).\
-   Create **triggers** to update them.\
-   **Process messages** against triggers.\
-   **Append stats** to every AI reply.\
-   Optionally, inject stats back into the prompt for smarter RP.

This system creates **game-like interactions** in character AI chats.

------------------------------------------------------------------------
