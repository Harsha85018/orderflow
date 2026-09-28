resource "aws_ecr_repository" "service" {
  for_each = toset(var.services)

  name = "${var.project}/${each.key}"

  # A tag can never be overwritten, so a deployed tag always means the same image.
  image_tag_mutability = "IMMUTABLE"

  # Lets terraform destroy remove repositories that still contain images.
  force_delete = true

  image_scanning_configuration {
    scan_on_push = true
  }
}

resource "aws_ecr_lifecycle_policy" "service" {
  for_each = aws_ecr_repository.service

  repository = each.value.name
  policy = jsonencode({
    rules = [{
      rulePriority = 1
      description  = "Keep only the 10 most recent images"
      selection = {
        tagStatus   = "any"
        countType   = "imageCountMoreThan"
        countNumber = 10
      }
      action = {
        type = "expire"
      }
    }]
  })
}
