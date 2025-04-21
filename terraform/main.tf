terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 4.16"
    }
    cloudflare = {
      source  = "cloudflare/cloudflare"
      version = "~> 5"
    }
  }

  backend "s3" {
    bucket       = "packtrain-terraform"
    key          = "tfstate"
    region       = "us-west-2"
    use_lockfile = true
  }

  required_version = ">= 1.2.0"
}

provider "aws" {
  region = var.aws_region
}

