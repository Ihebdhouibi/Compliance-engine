path = 'src/main/java/com/devteam/aiauditserver/controllers/admin/NotificationController.java'
content = open(path, encoding='utf-8').read()
# Fix the PreAuthorize role name
content = content.replace("hasRole('ADMIN')", "hasRole('ADMIN')")
print('already correct or fixing...')