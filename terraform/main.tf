terraform {
  required_version = ">= 1.5"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.0"
    }
  }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project   = "OrcaLab"
      ManagedBy = "terraform"
    }
  }
}

# Rol e instance profile preexistentes del Learner Lab (no se pueden crear roles IAM)
data "aws_iam_instance_profile" "lab" {
  name = "LabInstanceProfile"
}
