USE [contAInBallenbak]
GO

SET ANSI_NULLS ON
GO

DROP TABLE  [dbo].[tblSPDeltalinkRepository]
GO

/****** Object:  Table [dbo].[tblSPDeltalinkRepository]    Script Date: 27/01/2026 12:17:48 ******/
SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

CREATE TABLE [dbo].[tblSPDeltalinkRepository](
	[LogDateTime] [datetime] NOT NULL,
	[SourceID] [varchar](256) NOT NULL,
	[TokenID] [varchar](max) NOT NULL
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO


