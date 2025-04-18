variable "packtrain_cidr" {
  type    = string
  default = "10.0.0.0/16"
}

variable "public_subnet_cidr" {
  type    = list(string)
  default = ["10.0.1.0/24", "10.0.2.0/24"]
}

variable "private_subnet_cidr" {
  type    = list(string)
  default = ["10.0.101.0/24", "10.0.102.0/24"]
}

variable "availability_zones" {
  description = "AZs to spread subnets across"
  type        = list(string)
  default     = ["us-west-2a", "us-west-2b"]
}

resource "aws_vpc" "packtrain_vpc" {
  cidr_block           = var.packtrain_cidr
  enable_dns_support   = true
  enable_dns_hostnames = true
}

resource "aws_subnet" "public" {
  for_each                = zipmap(var.availability_zones, var.public_subnet_cidr)
  vpc_id                  = aws_vpc.packtrain_vpc.id
  cidr_block              = each.value
  availability_zone       = each.key
  map_public_ip_on_launch = true # automatically assign public IPs
}

resource "aws_internet_gateway" "igw" {
  vpc_id = aws_vpc.packtrain_vpc.id
}

resource "aws_route" "public_internet" {
  route_table_id         = aws_route_table.public.id
  destination_cidr_block = "0.0.0.0/0"
  gateway_id             = aws_internet_gateway.igw.id
}

resource "aws_subnet" "private" {
  for_each          = zipmap(var.availability_zones, var.private_subnet_cidr)
  vpc_id            = aws_vpc.packtrain_vpc.id
  cidr_block        = each.value
  availability_zone = each.key
}

resource "aws_eip" "nat_eip" {
  vpc = true
}

resource "aws_nat_gateway" "nat" {
  allocation_id = aws_eip.nat_eip.id
  subnet_id     = aws_subnet.public[var.availability_zones[0]].id
}

resource "aws_route_table" "private" {
  vpc_id = aws_vpc.packtrain_vpc.id
}
resource "aws_route_table" "public" {
  vpc_id = aws_vpc.packtrain_vpc.id
}

resource "aws_route" "private_nat" {
  route_table_id         = aws_route_table.private.id
  destination_cidr_block = "0.0.0.0/0"
  nat_gateway_id         = aws_nat_gateway.nat.id
}

resource "aws_route_table_association" "private_assoc" {
  for_each       = aws_subnet.private
  subnet_id      = each.value.id
  route_table_id = aws_route_table.private.id
}

resource "aws_route_table_association" "public_assoc" {
  for_each       = aws_subnet.public
  subnet_id      = each.value.id
  route_table_id = aws_route_table.public.id
}

