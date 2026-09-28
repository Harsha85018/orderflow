terraform {
  required_version = ">= 1.10"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.0"
    }
  }

  # State stays local for now. For a team, this would move to an S3 backend
  # with native locking (use_lockfile = true), so two people can't apply at once.
}

provider "aws" {
  region = var.region

  # Every resource gets these tags, which makes cost tracking and cleanup easy.
  default_tags {
    tags = {
      Project   = var.project
      ManagedBy = "terraform"
    }
  }
}
