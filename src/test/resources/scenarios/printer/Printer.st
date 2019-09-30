
0 -f-> Client:main.
(
    Client -pageRequest-> Printer:preparePage<pages >= 0>.
    Rel(Client, pageRequest).
    Printer{
        Printer resolves pageRequest with Ok.
            Client fetches pageRequest as Ok.
            Client -printJob-> Printer:print.
                Printer resolves printJob.
            Client -finalization-> Printer:finishPage.
            Rel(Client, finalization).
                Printer resolves finalization,
        Printer resolves pageRequest with OutOfPaper.
            Client fetches pageRequest as OutOfPaper
    }
)*.
Client -printPrinterTrace-> Printer:printTrace.
Rel(Client, printPrinterTrace).
    Printer resolves printPrinterTrace.
Client resolves f