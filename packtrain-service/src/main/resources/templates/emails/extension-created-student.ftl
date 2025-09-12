<!DOCTYPE html>
<html>
    <head>
      <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    </head>
    <body>
      <p>Hi ${requester},</p>
      <#if extension>
          <p>Your extension request for ${extensionDays} day(s) for assignment ${assignmentName} was received and sent to Professor ${instructor} for approval.</p>
          <p></p>
          <p><strong>This extension has not yet been approved.</strong></p>
      <#else>  
          <p>Your late pass request for ${extensionDays} day(s) for assignment ${assignmentName} has been automatically approved.</p>
      </#if>
      <p></p>
      <p>If you have any questions, please contact the course staff.</p>
    </body>
</html>
