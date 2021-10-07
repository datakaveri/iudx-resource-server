package iudx.resource.server.admin.util;

public class Constants {

  public static final String INSERT_UNIQUE_ATTRIBUTE =
      "INSERT INTO table_name (id, uniqueAddress, createdAt, updatedAt) " +
          "VALUES ('$1', '$2', '$3', '$4')";

  public static final String DELETE_UNIQUE_ATTRIBUTE =
      "DELETE FROM table_name WHERE id = '$1'";
}
