export const formattedDate = (date: Date) => {
  return date
    .toLocaleString("en-US", {
      month: "long",
      day: "2-digit",
      year: "numeric",
      hour: "numeric",
      minute: "2-digit",
      hour12: true,
    })
    .replace(" at ", ", ");
};

export const calculateNewDueDate = (date: Date, days: number) => {
  const newDate = new Date(date);
  newDate.setDate(newDate.getDate() + days);
  return newDate;
};
