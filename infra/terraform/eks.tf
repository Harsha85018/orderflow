module "eks" {
  source  = "terraform-aws-modules/eks/aws"
  version = "~> 21.0"

  name               = var.project
  kubernetes_version = var.kubernetes_version

  # kubectl from your laptop needs to reach the API server.
  endpoint_public_access = true

  # Only our own IP may reach the public endpoint (fixes Trivy AWS-0041).
  endpoint_public_access_cidrs = var.api_allowed_cidrs

  # Nodes reach the control plane privately, inside the VPC. Without this,
  # the IP restriction above would also lock the worker nodes out.
  endpoint_private_access = true

  # Whoever runs terraform apply gets admin access to the cluster.
  enable_cluster_creator_admin_permissions = true

  vpc_id     = module.vpc.vpc_id
  subnet_ids = module.vpc.public_subnets

  addons = {
    coredns    = {}
    kube-proxy = {}
    vpc-cni = {
      before_compute = true
    }
    eks-pod-identity-agent = {
      before_compute = true
    }
  }

  eks_managed_node_groups = {
    default = {
      ami_type       = "AL2023_ARM_64_STANDARD"
      instance_types = var.node_instance_types

      # Spot: spare AWS capacity at a large discount. Nodes can be reclaimed
      # with 2 minutes' notice, which is fine for a test environment and a
      # good real-world test of the self-healing we already verified.
      capacity_type = "SPOT"

      min_size     = 1
      max_size     = 3
      desired_size = var.node_count
    }
  }
}
