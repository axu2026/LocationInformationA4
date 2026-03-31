# Assignment 4: Location Information

This app is a demonstration of the Google map component for Android, as well as the use of Geocoding for information and the location permission of the device. The app will ask the user for permission to get their location, and upon approval will show the location with a marker. Tapping on the marker will give information about the location. The user can also tap around the map to place custom markers too.

This was tested with my own Google Cloud API key. It has been removed here, but place your own in the AndroidManifest.xml file.

<img width="575" height="889" alt="Screenshot 2026-03-30 223152" src="https://github.com/user-attachments/assets/4054d3ce-cdf3-4363-923c-b7dc2acd3779" />
<img width="637" height="857" alt="Screenshot 2026-03-30 222734" src="https://github.com/user-attachments/assets/b8df330c-035e-4b15-806a-2914a57d87ee" />
<img width="641" height="905" alt="Screenshot 2026-03-30 222717" src="https://github.com/user-attachments/assets/d8641886-29d3-4189-971a-dd97819117b7" />

# AI Disclosure

AI (ChatGPT/Gemini) was used to debug issues with the emulator not working with my default emulator. I was able to get it to work by using a Google Play version of the emulator (rather than the Google API ones) and including some lines in the manifest file to use a library that Google maps required.
