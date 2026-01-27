USE [contAInBallenbak]
GO

/****** Object:  Table [dbo].[tblIOLog]    Script Date: 06/01/2026 12:26:18 ******/
SET ANSI_NULLS ON
GO

DROP TABLE  [dbo].[tblIOLog]
GO
SET QUOTED_IDENTIFIER ON
GO

CREATE TABLE [dbo].[tblIOLog](
	
	[UUID] [varchar](36) NOT NULL,
	[containIOUUID] [varchar](36) NOT NULL,
	[PlatformID] [varchar](36) NULL,
	[Path][varchar](max) NULL,
	[IOAction] [varchar](max) NOT NULL,
	[IOSource] [varchar](50) NOT NULL,
	[IODestination] [varchar](50) NOT NULL,
	[PKIHash] varchar(max) NULL,
	[IOreference] [varchar](50) NOT NULL,
	[AdditionalInfo] [varchar](max) NULL,
	[LogDateTime] [datetime] NOT NULL,
	[ActionPerformed] [Varchar](20) NOT NULL,
	CONSTRAINT chk_AP CHECK ([ActionPerformed] IN ('IOMOVED', 'ASSIGNUUID', 'IORENAMED', 'IOCLASSIFIED','COPIEDUUID','IODELETED', 'IOCOPIED','IOBOUND')),
	[ActionPerformedBy] [Varchar](50) NOT NULL
	CONSTRAINT [PK_tblIOLog] PRIMARY KEY CLUSTERED
 (
	[UUID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO

ALTER TABLE [dbo].[tblIOLog]  WITH CHECK ADD  CONSTRAINT [FK_tblIOLog_tblIOLog] FOREIGN KEY([UUID])


REFERENCES [dbo].[tblIOLog] ([UUID])
GO

ALTER TABLE [dbo].[tblIOLog] CHECK CONSTRAINT [FK_tblIOLog_tblIOLog]
GO