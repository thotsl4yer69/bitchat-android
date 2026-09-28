# BitNow Android Privacy Policy

*Last updated: 29 September 2026*

BitNow is a proximity-first, accountless dating client built on the BitChat Android transport. The Android app is designed so that nearby discovery and dating state work without a BitNow-operated central profile database.

## Summary

BitNow Android does not require a phone number, email address, cloud dating account or central profile service.

Dating profiles, preferences, availability timers, outgoing signals, blocks and local safety records are stored on the device. When you deliberately go live, selected profile data and dating signals are shared only with nearby peers that are also actively advertising BitNow support, using the existing authenticated encrypted private-message transport.

BitNow does not place your age, dating intent, identity, preferences, profile text or precise location in the public BLE announcement. The public announcement contains only protocol/feature capability bits, including a BitNow availability bit while your time-limited visibility window is active.

## Information stored on your device

BitNow Android can store:

- your display name and adult age;
- whether to show your exact age or only "18+";
- optional identity, pronouns, headline and profile/about text;
- the identities you are open to meeting;
- your selected intent, such as meet now, tonight, meet first or chat first;
- your discovery filters;
- the expiry time for your current availability window;
- outgoing dating signals, which expire after 45 minutes;
- blocked peer identifiers;
- local safety-report records created by "Report & block";
- BitChat identity keys, conversation state and other transport data used by the underlying app.

The first BitNow profile is saved hidden. Nearby dating visibility starts only after you explicitly choose a 30-minute, 1-hour, 2-hour or 4-hour availability window. Expiry turns dating visibility off and clears active dating signals.

## Information shared with nearby BitNow peers

While you are live, BitNow can share the following through authenticated encrypted one-to-one control messages:

- age, or "18+" if you hide your exact age;
- optional identity and pronouns;
- headline and about/boundaries text;
- selected intent;
- the identities you are open to meeting;
- a dating signal when you choose "Interested."

BitNow does not broadcast those profile fields in the BLE announcement itself. Android shares dating controls only with peers that are also actively advertising the BitNow capability.

Nearby cards use coarse Bluetooth signal bands such as "very close", "close" or "nearby". BitNow does not present a precise distance or map pin from BLE signal strength.

## Matches and messages

A BitNow match exists when both peers have active dating signals for each other. Signals expire automatically after 45 minutes.

Direct messages use the existing BitChat encrypted private-message path. BitNow dating controls use that same authenticated encrypted transport and are consumed as application metadata rather than displayed as chat messages.

## Blocks and reports

Blocking removes the peer from BitNow discovery and clears local dating state for that peer. Android's current "Report & block" flow stores a bounded safety record locally on the device and blocks the peer. This Android build does not silently upload that report to a BitNow server.

Erasing the dating profile and preferences does not erase safety blocks or safety-report records. This prevents a privacy reset from unintentionally re-exposing blocked peers.

## Location and nearby permissions

BitNow uses Bluetooth discovery to find nearby compatible peers. Android versions and device vendors may require Nearby Devices and/or Location permissions for BLE scanning.

The underlying BitChat transport also contains optional geohash/Nostr features. If you use a feature that derives a geohash from device location, the app may access location locally to calculate that coarse area. Precise GPS coordinates are not part of the BitNow dating profile or BitNow BLE capability announcement.

## Network transport

Core nearby BitNow discovery can operate over BLE without a BitNow-operated central dating service. The underlying BitChat client can also use internet transports such as Nostr relays when those features are enabled. Data sent to third-party decentralized relays is subject to the behavior and retention of those relays.

## Age restriction

BitNow is for adults aged 18 or older. Profile creation requires an 18+ confirmation and an age from 18 through 99. The app also rejects selected profile text patterns that explicitly claim the user is under 18.

This local gate is not government-ID age verification and should not be represented as one.

## Your controls

You can:

- hide immediately at any time;
- allow a visibility window to expire automatically;
- clear active dating signals;
- edit your profile and filters;
- block or report-and-block a peer;
- erase your BitNow dating profile, preferences, signals and discovery filters from the device while preserving safety blocks.

Deleting the application or clearing its app data removes ordinary local application state according to Android's storage behavior.

## Analytics and advertising

The BitNow layer added by this project does not add advertising SDKs or behavioral analytics. Upstream transport code and any optional internet relay behavior remain separately auditable in this repository.

## Source and provenance

BitNow Android is based on the open-source BitChat Android project. Upstream provenance and licensing are documented in `UPSTREAM.md` and `UPSTREAM_COMMIT`.
