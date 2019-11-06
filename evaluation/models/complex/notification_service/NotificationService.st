0 -fInit-> NotificationService:init.
(
  NotificationService -fCheckMail-> MailServer:checkMail.
  Rel(NotificationService, fCheckMail).
  MailServer {
    MailServer resolves fCheckMail with NewMail.
      NotificationService fetches fCheckMail as NewMail.
      NotificationService -fPopup-> UI:popup.
        UI resolves fPopup,
    MailServer resolves fCheckMail with NoMail.
      NotificationService fetches fCheckMail as NoMail
  }
)*.
NotificationService resolves fInit
