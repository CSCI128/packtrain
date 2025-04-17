resource "aws_iam_role" "ecs_task_execution" {
  name = "ecsTaskExecutionRole"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "ecs-tasks.amazonaws.com" }
      Action    = "sts:AssumeRole"
    }]
  })

  managed_policy_arns = [
    "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
  ]
}

resource "aws_iam_policy" "s3_policy" {
  name_prefix = "s3_policy_"
  description = "Policy for S3 access to a specific bucket"
  policy = jsonencode({
    "Version" : "2012-10-17",
    "Statement" : [
      {
        "Sid" : "Stmt1744917987457",
        "Action" : [
          "s3:CreateBucket",
          "s3:DeleteObject",
          "s3:PutObject"
        ],
        "Effect" : "Allow",
        "Resource" : "arn:aws:s3:::*"
      }
  ] })
}

resource "aws_iam_user" "s3_user" {
  name = "s3_user"
}

resource "aws_iam_group" "s3_group" {
  name = "s3_group"
}

resource "aws_iam_group_membership" "s3_user_membership" {
  name  = "s3_group_membership"
  users = [aws_iam_user.s3_user.name]
  group = aws_iam_group.s3_group.name
}

resource "aws_iam_group_policy_attachment" "s3_group_policy_attach" {
  group      = aws_iam_group.s3_group.name
  policy_arn = aws_iam_policy.s3_policy.arn
}

resource "aws_iam_access_key" "s3_access_key" {
  user = aws_iam_user.s3_user.name
}

output "access_key_id" {
  value = aws_iam_access_key.s3_access_key.id
}

output "secret_access_key" {
  value     = aws_iam_access_key.s3_access_key.secret
  sensitive = true
}
