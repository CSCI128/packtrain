import {
  Center,
  Group,
  keys,
  Table,
  Text,
  UnstyledButton,
} from "@mantine/core";
import {
  IconChevronDown,
  IconChevronUp,
  IconSelector,
} from "@tabler/icons-react";
import React from "react";
import classes from "./Table.module.scss";

interface TableHeaderProps {
  children: React.ReactNode;
  reversed: boolean;
  sorted: boolean;
  onSort: (() => void) | undefined;
}

export function filterData<T extends Record<string, any>>(
  data: T[],
  search: string
) {
  const query = search.toLowerCase().trim();
  return data.filter((item) =>
    keys(data[0]!).some((key) =>
      String(item[key]).toLowerCase().includes(query)
    )
  );
}

export function sortData<T extends Record<string, any>>(
  data: T[],
  payload: {
    sortBy: keyof T | null;
    reversed: boolean;
    search: string;
  }
) {
  const { sortBy, reversed, search } = payload;

  if (!sortBy) {
    return filterData(data, search);
  }

  return filterData(
    [...data].sort((a, b) => {
      const aValue = a[sortBy];
      const bValue = b[sortBy];

      const aNum = parseFloat(aValue);
      const bNum = parseFloat(bValue);
      const bothAreNumbers = !isNaN(aNum) && !isNaN(bNum);

      let comparison = 0;

      if (bothAreNumbers) {
        comparison = aNum - bNum;
      } else {
        comparison = String(aValue).localeCompare(String(bValue));
      }

      return reversed ? -comparison : comparison;
    }),
    search
  );
}

export function TableHeader({
  children,
  reversed,
  sorted,
  onSort,
}: TableHeaderProps) {
  const Icon = sorted
    ? reversed
      ? IconChevronUp
      : IconChevronDown
    : IconSelector;
  return (
    <Table.Th className={classes.th}>
      {onSort !== undefined ? (
        <UnstyledButton onClick={onSort} className={classes.control}>
          <Group justify="space-between">
            <Text fw={500} fz="sm">
              {children}
            </Text>
            <Center className={classes.icon}>
              <Icon size={16} stroke={1.5} />
            </Center>
          </Group>
        </UnstyledButton>
      ) : (
        <Text fw={500} fz="sm" ta="center">
          {children}
        </Text>
      )}
    </Table.Th>
  );
}
