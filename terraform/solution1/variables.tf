variable "region" {
  description = "AWS region"
  type        = string
  default     = "eu-west-2"
}

variable "app_name" {
  description = "Base name for ECS/ECR resources"
  type        = string
  default     = "products-suggestions-app-ec"
}

variable "image_tag" {
  description = "Tag of the image pushed to ECR"
  type        = string
  default     = "latest"
}

variable "desired_count" {
  description = "How many tasks to run in the ECS service"
  type        = number
  default     = 3
}

variable "container_port" {
  description = "Port your app listens on inside the container"
  type        = number
  default     = 8080
}

variable "task_cpu" {
  description = "Fargate task CPU units"
  type        = number
  default     = 256
}

variable "task_memory" {
  description = "Fargate task memory (MB)"
  type        = number
  default     = 512
}