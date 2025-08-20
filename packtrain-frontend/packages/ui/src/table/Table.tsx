import { Center, Group, Table, Text, UnstyledButton } from "@mantine/core";
import {
  IconChevronDown,
  IconChevronUp,
  IconSelector,
} from "@tabler/icons-react";
import React, { useMemo, useState } from "react";
import classes from "./Table.module.scss";

interface TableHeaderProps {
  children: React.ReactNode;
  reversed: boolean;
  sorted: boolean;
  onSort: (() => void) | undefined;
}

export function useTableData<T extends Record<string, any>>(rawData: T[]) {
  const [search, setSearch] = useState("");
  const [sortBy, setSortBy] = useState<string | null>(null);
  const [reverseSortDirection, setReverseSortDirection] = useState(false);

  const sortedData = useMemo(
    () =>
      sortData(rawData, {
        sortBy,
        reversed: reverseSortDirection,
        search,
      }),
    [rawData, sortBy, reverseSortDirection, search]
  );

  const handleSearchChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    setSearch(event.currentTarget.value);
  };

  const resetTable = () => {
    setSearch("");
    setSortBy(null);
    setReverseSortDirection(false);
  };

  const handleSort = (field: string) => {
    setReverseSortDirection(field === sortBy ? !reverseSortDirection : false);
    setSortBy(field);
  };

  return {
    search,
    sortBy,
    reverseSortDirection,
    sortedData,
    handleSearchChange,
    handleSort,
    resetTable,
  };
}

// resolve nested keys by "."
function getValue<T extends Record<string, any>>(obj: T, path: string): any {
  return path.split(".").reduce((acc, key) => acc?.[key], obj);
}

// search all values
function getAllValues(obj: Record<string, any>): string[] {
  const values: string[] = [];

  function recurse(current: any) {
    if (current == null) return;
    if (typeof current === "object") {
      for (const key in current) {
        recurse(current[key]);
      }
    } else {
      values.push(String(current));
    }
  }

  recurse(obj);
  return values;
}

export function filterData<T extends Record<string, any>>(
  data: T[],
  search: string
) {
  const query = search.toLowerCase().trim();
  if (query.length === 0) return data;

  return data.filter((item) =>
    getAllValues(item).some((value) => value.toLowerCase().includes(query))
  );
}

export function sortData<T extends Record<string, any>>(
  data: T[],
  payload: {
    sortBy: string | null;
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
      let aValue = getValue(a, sortBy);
      let bValue = getValue(b, sortBy);

      let comparison = 0;

      // Check if both values are parseable as dates
      const aDate = new Date(aValue);
      const bDate = new Date(bValue);
      const bothAreDates = !isNaN(aDate.getTime()) && !isNaN(bDate.getTime());

      if (bothAreDates) {
        comparison = aDate.getTime() - bDate.getTime();
      } else {
        const aNum = parseFloat(aValue);
        const bNum = parseFloat(bValue);
        const bothAreNumbers = !isNaN(aNum) && !isNaN(bNum);

        if (bothAreNumbers) {
          comparison = aNum - bNum;
        } else {
          comparison = String(aValue).localeCompare(String(bValue));
        }
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
