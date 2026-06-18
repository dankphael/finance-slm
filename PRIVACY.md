# Privacy Policy — Finance SLM

_Last updated: 18 June 2026_

Finance SLM is a privacy-first Android app. **All processing happens on your
device. No financial data, screen content, or generated insight ever leaves
your phone.** There are no analytics, no tracking, and no remote servers that
receive your data.

## What the app accesses

### Accessibility Service (screen reading)
With your explicit consent, Finance SLM uses Android's AccessibilityService to
read on-screen text **only from a fixed allow-list of Singapore finance apps**
(DBS/PayNow, PayLah!, OCBC, UOB, Grab, Moomoo, Tiger Brokers). The service:

- Is restricted at the system level to those specific app packages.
- Skips password fields (`isPassword`) so credentials are never read.
- Ignores notification events, which can contain OTPs and one-time codes.
- Explicitly excludes Finance SLM's own package.

The extracted text is used solely to build a prompt for the **on-device**
language model that generates your financial insights.

### Network
The app uses the internet for **one purpose only**: downloading the language
model files you choose from Hugging Face. Your financial data is never sent
over the network.

## What is stored, and where

- Generated insights are stored in a local database on your device.
- Recently captured screen text is held in a small local cache.
- Downloaded model files are stored in the app's private storage.

`allowBackup` is disabled, so this data is not included in cloud backups.

## Your controls

From **Settings** inside the app you can at any time:

- **Export** all of your insights as a JSON file.
- **Delete all data** — insights and the screen-data cache — with one tap.

You can also revoke the Accessibility permission at any time from Android's
system settings, which immediately stops all screen reading.

## Contact

For questions about this policy, contact the project maintainer through the
project's repository.
