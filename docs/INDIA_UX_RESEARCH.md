# Indian adult usability: research and first changes

Research reviewed 26 September 2026. These are evidence-informed design choices, not a claim that one experience is best for all Indian users or everyone over 30.

## Evidence

- [IAMAI/Kantar, Internet in India 2024](https://www.iamai.in/sites/default/files/research/Kantar_%20IAMAI%20report_2024_.pdf): widespread Indic-language use supports making language choice visible. Hindi alone does not cover India; choose additional languages from the intended audience.
- [W3C older-user guidance](https://www.w3.org/WAI/older-users/): vision, dexterity and cognitive accessibility are relevant to older users. Age is not itself a disability or a substitute for testing.
- [Android accessibility guidance](https://developer.android.com/guide/topics/ui/accessibility/views/apps-views): labelled interactions and at least 48dp touch targets inform the control changes.

## Applied to Android

Visible Hindi/English switch; guidance raised to 16sp; ritual buttons raised to 52dp with 16sp labels; a tap alternative to swipe cleaning and an explicit flower-offering button; existing ordered, one-action-at-a-time ritual retained. Completion now has a clear message and a progress/package action. Account screen scrolls, uses labelled fields, explains data collection, and separates free puja from optional customization. No payment or sharing interruption during worship. Neutral language when a streak is missed, with permanent retention of earned unlocks.

The account screen explains six package categories and three intended unlock routes. Purchase remains visibly unavailable until verified billing exists. A seven-day streak or one qualified invitation is a product assumption, not a researched universal preference.

## Next validation

Test with users aged 30–44, 45–59, and 60+, varying reading language, digital confidence, eyesight, motor ability and device quality. Include both experienced and first-time worship-app users. Ask them to change language, complete a puja without gestures, recover from interruptions, understand the free vs paid offer, sign in, explain reward requirements and delete their account.

Measure task completion, mistaken taps, help requests and comprehension, rather than only session length. Test TalkBack, 200% system font scaling, small screens, low bandwidth and process death. Revisit the existing automatic step advance and progress-panel height after device testing. Consider optional spoken guidance, reduced motion and phone OTP after validation; these are not implemented in this pass.
