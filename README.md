Java library for interacting with Steam Authenticator.

Serializing from .mafile:
Gson gson = new GsonBuilder().serializeNulls().create();
SteamGuard steamGuard = gson.fromJson(mafileString, SteamGuard.class);


As for now implemented:

• generation of two factor code;

• login into account for refreshing session;

• refreshing session with Oauthtoken(if it's possible);

• fetching confirmations

• accepting confirmations

Future updates:

• linking phone to account;

• accepting array of confirmations at one request;

Examples

Generating 2FA:

steamGuard.GenerateSteamGuardCodeForTime(Util.GetSystemUnixTime());

Login into account(Without SteamGuard):

UserLogin user = new UserLogin("bn2nw1o6z", "mbfbc9pvg");
user.DoLogin()

Login into account(With SteamGuard):

UserLogin user = new UserLogin("bn2nw1o6z", "mbfbc9pvg");
user.TwoFactorCode = steamGuard.GenerateSteamGuardCodeForTime(Util.GetSystemUnixTime());
user.DoLogin()

Refreshing session:

steamGuard.RefreshSession();

Fetch confirmations:

Confirmation[] confirmations =  steamGuard.FetchConfirmations();

Accept confirmation:

steamGuard.AcceptConfirmation(confirmations[0]);

