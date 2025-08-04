variable "region" {
  description = "AWS region"
  type        = string
  default     = "eu-west-2"
}

variable "app_name" {
  description = "The name of the application"
  type        = string
}

variable "app_version" {
  description = "The version of the application"
  type        = string
}
