variable "project" {
  description = "Name used for the cluster, repositories, and tags."
  type        = string
  default     = "orderflow"
}

variable "region" {
  description = "AWS region to deploy into."
  type        = string
  default     = "us-east-1"
}

variable "kubernetes_version" {
  description = "EKS Kubernetes version. Check supported versions before applying."
  type        = string
  default     = "1.34"
}

variable "node_instance_types" {
  description = "ARM (Graviton) instance types, matching images built on Apple Silicon. Several types make Spot capacity easier to get."
  type        = list(string)
  default     = ["t4g.large", "m7g.large", "m6g.large"]
}

variable "node_count" {
  description = "Number of worker nodes."
  type        = number
  default     = 2
}

variable "services" {
  description = "One ECR repository is created per service."
  type        = list(string)
  default     = ["order-service", "payment-service", "inventory-service", "notification-service"]
}

variable "budget_usd" {
  description = "Monthly cost budget in USD for alert emails."
  type        = string
  default     = "20"
}

variable "alert_email" {
  description = "Where budget alerts are sent. Set in terraform.tfvars (not committed)."
  type        = string
}
