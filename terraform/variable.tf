variable "aws_region" {
  type    = string
  default = "us-west-2"
}

variable "authentik_image" {
  description = "Docker image for authentik"
  type        = string
  default     = "beryju/authentik:2025.2.4"
}

variable "policy_image" {
  description = "Docker image for grading policy server"
  type        = string
  default     = "ghcr.io/csci128/packtrain/policy-server:main"
}

variable "authentik_db_user" {
  type    = string
  default = "authentik"
}

variable "authentik_db_name" {
  type    = string
  default = "authentik"
}

variable "packtrain_db_user" {
  type    = string
  default = "packtrain"
}

variable "packtrain_db_name" {
  type    = string
  default = "packtrain"
}

variable "app_domain" {
  type    = string
  default = "packtrain.gregory-bell.com"
}

variable "zone_domain" {
  type    = string
  default = "gregory-bell.com"
}

variable "mq_username" {
  type    = string
  default = "brokeradmin"
}
