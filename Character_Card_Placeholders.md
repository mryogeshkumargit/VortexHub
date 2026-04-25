# Character Card Placeholders (Macros) Reference

This document explains the use of dynamic placeholders (`{{...}}`) in
character cards (SillyTavern v3, TavernAI, Pygmalion, etc.).\
These placeholders make characters more immersive by automatically
inserting names, context, and other variables during roleplay.

------------------------------------------------------------------------

## 🔹 Core Placeholders

  ----------------------------------------------------------------------------------------------------------------
  Tag              Meaning                     Example
  ---------------- --------------------------- -------------------------------------------------------------------
  `{{user}}`       The user's name             `"Hello {{user}}" → "Hello Yogesh"`

  `{{char}}`       The character's name        `"I am {{char}}" → "I am Aisha"`

  `{{user_msg}}`   Last user message           `"{{char}} repeats {{user_msg}}" → "Aisha repeats: How are you?"`

  `{{last_msg}}`   Last message in             Shows whichever was last
                   conversation (user or char) 
  ----------------------------------------------------------------------------------------------------------------

------------------------------------------------------------------------

## 🔹 Message Context Placeholders

  Tag                      Meaning
  ------------------------ --------------------------------------------
  `{{history}}`            Inserts entire conversation history.
  `{{recent_messages}}`    Inserts a limited recent chunk of history.
  `{{system}}`             Inserts hidden system prompt/instructions.
  `{{description}}`        Character's description from the card.
  `{{personality}}`        Character's personality traits.
  `{{scenario}}`           The initial scenario from the card.
  `{{example_dialogue}}`   Example chats included in the card.

------------------------------------------------------------------------

## 🔹 Random / Utility Placeholders

  -----------------------------------------------------------------------
  Tag                       Meaning
  ------------------------- ---------------------------------------------
  `{{random_char}}`         Random character name (useful for multi-char
                            RP).

  `{{random_user}}`         Random user name.

  `{{time}}`                Inserts current time.

  `{{date}}`                Inserts current date.
  -----------------------------------------------------------------------

------------------------------------------------------------------------

## 🔹 Advanced / Custom Placeholders

You can define **custom variables** inside a character card or lorebook
and then call them later.

Example definition in **Lorebook**:

``` json
{
  "key": "city",
  "value": "Kasganj"
}
```

Usage inside card or RP text:

    "We are in {{city}}" → "We are in Kasganj"

Some forks also support **conditional macros** like:

    {{#if user}}Hello {{user}}{{/if}}

(Support varies depending on frontend.)

------------------------------------------------------------------------

## 🔹 Example in Action

Suppose the card has:

-   **User Name**: Yogesh\
-   **Char Name**: Aisha\
-   **Scenario**: `"{{char}} is meeting {{user}} in {{city}}."`\
-   **Custom var**: `"city": "Kasganj"`

Conversation start might render as:

    Aisha is meeting Yogesh in Kasganj.

------------------------------------------------------------------------

## 🔹 Summary

-   Use `{{user}}` and `{{char}}` for names.\
-   Use `{{history}}` and `{{recent_messages}}` for context.\
-   Add `{{time}}` and `{{date}}` for realism.\
-   Create custom variables (`{{city}}`, `{{pet_name}}`, etc.) for
    immersive storytelling.
