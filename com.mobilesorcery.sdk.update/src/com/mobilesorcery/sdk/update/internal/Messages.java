package com.mobilesorcery.sdk.update.internal;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "com.mobilesorcery.sdk.update.internal.messages"; //$NON-NLS-1$
	public static String ConfirmationDialog_CheckUpdates;
	public static String ConfirmationDialog_Description;
	public static String ConfirmationDialog_Resend;
	public static String ConfirmationDialog_Title;
	public static String RegistrationDialog_CheckUpdatesCaption;
	public static String RegistrationDialog_Description;
	public static String RegistrationDialog_EmailCaption;
	public static String RegistrationDialog_NameCaption;
	public static String RegistrationDialog_SubscriptionCaption;
	public static String RegistrationDialog_Title;
	public static String ResendConfirmationCodeAction_ConfirmationEmailError;
	public static String ResendConfirmationCodeAction_ConfirmationEmailMessage;
	public static String ResendConfirmationCodeAction_JobTitle;
	public static String UpdateAvailablePage_Message;
	public static String UpdateAvailablePage_Title;
	public static String UpdateMessagePage_Message;
	public static String UpdateMessagePage_Title;
	public static String UpdateProfilesAction_ConfirmTitle;
	public static String UpdateProfilesAction_InformationTitle;
	public static String UpdateProfilesAction_InvalidConfirmationCode;
	public static String UpdateProfilesAction_NoUpdatesAvailable;
	public static String UpdateProfilesAction_UpdateProfiles;
	public static String UpdateProfilesAction_IOError;
	public static String UpdateProfilesAction_CouldNotConnect;
	public static String UpdateProfilesAction_AlreadyRegistered;
	public static String UpdateProfilesAction_ConfirmationCodeSent;
	public static String UpdateProfilesAction_InvalidEmail;
	public static String UpdateProfilesAction_Downloading;
	public static String UpdateProfilesAction_UpdateJobTitle;
	public static String UpdateWizard_RestartDialogMessage;
	public static String UpdateWizard_RestartDialogTitle;
	public static String UpdateWizard_WindowTitle;
	public static String UpdateWizardDialog_UpdateButton;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
