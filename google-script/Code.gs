function saveMailsAsEmlToGoogleDrive() {
  
  var folderName = 'emails'
  var labelName = 'Personal/Gmail/savedToEml'
  
  var controlLabel = GmailApp.getUserLabelByName(labelName);
  
  var folder = DriveApp.getFoldersByName(folderName);
  if (folder.hasNext()) {
    folder = folder.next();
  } else {
    folder = DriveApp.createFolder(folderName);
  }
  
  var threads = GmailApp.search('from:simon.lamplugh@gmail.com -label:' + labelName);
  
  for (i in threads) {
    
    var messages = threads[i].getMessages();
    
    for (j in messages) {
      
      var subject = messages[j].getSubject();
      var rawContent = messages[j].getRawContent();
      
      //var blob = Utilities.newBlob(rawContent, 'messages/rfc822', subject + '.eml');
      var blob = Utilities.newBlob(
        rawContent, 
        'messages/rfc822', 
        Math.random().toString(36)  + '.eml');
     
      folder.createFile(blob)
    }
  }
  
  for (i in threads) {
    controlLabel.addToThread(threads[i]);
  }
}
