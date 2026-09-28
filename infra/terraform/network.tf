data "aws_availability_zones" "available" {
  state = "available"
}

# Public subnets only, no NAT gateway: a NAT gateway costs money every hour it
# exists. Nodes get public IPs and are protected by security groups instead.
# A production setup would use private subnets with NAT or VPC endpoints.
module "vpc" {
  source  = "terraform-aws-modules/vpc/aws"
  version = "~> 6.0"

  name = var.project
  cidr = "10.0.0.0/16"

  azs            = slice(data.aws_availability_zones.available.names, 0, 2)
  public_subnets = ["10.0.0.0/20", "10.0.16.0/20"]

  map_public_ip_on_launch = true
  enable_nat_gateway      = false
  enable_dns_hostnames    = true
  enable_dns_support      = true

  # Lets Kubernetes place internet-facing load balancers in these subnets.
  public_subnet_tags = {
    "kubernetes.io/role/elb" = 1
  }
}
