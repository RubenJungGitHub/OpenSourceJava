CREATE TABLE dbo.tblIOLog (
    UUID varchar(36) NOT NULL PRIMARY KEY,
    containIOUUID varchar(36) NOT NULL,
    PlatformID varchar(36),
    Path varchar(max),
    IOAction varchar(max) NOT NULL,
    IOSource varchar(50) NOT NULL,
    IODestination varchar(50) NOT NULL,
    PKIHash varchar(max),
    IOreference varchar(50) NOT NULL,
    AdditionalInfo varchar(max),
    LogDateTime datetime NOT NULL,
    ActionPerformed varchar(20) NOT NULL,
    ActionPerformedBy varchar(50) NOT NULL,
    CONSTRAINT chk_AP CHECK (
        ActionPerformed IN (
            'IOMOVED','ASSIGNUUID','IORENAMED','IOCLASSIFIED',
            'COPIEDUUID','IODELETED','IOCOPIED','IOBOUND'
        )
    )
);