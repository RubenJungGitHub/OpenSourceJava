cls
& "C:\Program Files\MongoDB\Connector for BI\2.14\bin\mongosqld.exe" `
  --config "C:\ContainOpenSource\OpenSourceJava\ils\receiver\src\main\resources\bi-config-VM.yaml" `
  --auth `
  --mongo-username "bi_user" `
  --mongo-password "bi_password" `
  --mongo-authenticationSource "admin" `
  --sampleNamespaces "ilstools.*"