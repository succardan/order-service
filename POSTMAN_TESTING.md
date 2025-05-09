# Instruções para Teste com Postman

Este documento contém instruções para testar a aplicação Order Service usando o Postman.

## Configuração Inicial

1. Iniciando a aplicação com o perfil mock:
   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=mock
   ```

2. Importando a coleção do Postman (opcional):
   - Você pode criar uma nova coleção no Postman com os endpoints descritos abaixo

## Endpoints Disponíveis

### 1. Criar um Pedido

- **Método**: POST
- **URL**: http://localhost:8080/api/orders
- **Body** (JSON):
  ```json
  {
    "orderNumber": "ORD-TEST-101",
    "items": [
      {
        "productId": "PROD-1",
        "quantity": 2
      },
      {
        "productId": "PROD-2",
        "quantity": 1
      }
    ]
  }
  ```
- **Resposta Esperada** (200 OK):
  ```json
  {
    "id": "uuid-gerado",
    "orderNumber": "ORD-TEST-101",
    "status": "RECEIVED",
    "createdAt": "2025-05-08T10:00:00",
    "items": [
      {
        "productId": "PROD-1",
        "productName": "Produto 1",
        "quantity": 2,
        "price": 20.00
      },
      {
        "productId": "PROD-2",
        "productName": "Produto 2",
        "quantity": 1,
        "price": 30.00
      }
    ],
    "totalAmount": 70.00
  }
  ```

### 2. Obter um Pedido por Número

- **Método**: GET
- **URL**: http://localhost:8080/api/orders/{orderNumber}
- **Exemplo**: http://localhost:8080/api/orders/ORD-TEST-101
- **Resposta Esperada** (200 OK):
  ```json
  {
    "id": "uuid-gerado",
    "orderNumber": "ORD-TEST-101",
    "status": "CALCULATED",
    "createdAt": "2025-05-08T10:00:00",
    "processedAt": "2025-05-08T10:00:05",
    "items": [
      {
        "productId": "PROD-1",
        "productName": "Produto 1",
        "quantity": 2,
        "price": 20.00
      },
      {
        "productId": "PROD-2",
        "productName": "Produto 2",
        "quantity": 1,
        "price": 30.00
      }
    ],
    "totalAmount": 70.00
  }
  ```

### 3. Listar Todos os Pedidos

- **Método**: GET
- **URL**: http://localhost:8080/api/orders
- **Parâmetros Opcionais**:
  - `page`: Número da página (padrão: 0)
  - `size`: Tamanho da página (padrão: 20)
  - `status`: Filtro por status (RECEIVED, PROCESSING, CALCULATED, NOTIFIED, COMPLETED, ERROR)
- **Exemplo**: http://localhost:8080/api/orders?page=0&size=10&status=COMPLETED
- **Resposta Esperada** (200 OK): Lista de pedidos

### 4. Obter Status de um Pedido

- **Método**: GET
- **URL**: http://localhost:8080/api/orders/{orderNumber}/status
- **Exemplo**: http://localhost:8080/api/orders/ORD-TEST-101/status
- **Resposta Esperada** (200 OK):
  ```json
  {
    "orderNumber": "ORD-TEST-101",
    "status": "CALCULATED",
    "timestamp": "2025-05-08T10:00:05"
  }
  ```

### 5. Processar um Pedido Manualmente

- **Método**: POST
- **URL**: http://localhost:8080/api/orders/{id}/process
- **Exemplo**: http://localhost:8080/api/orders/123e4567-e89b-12d3-a456-426614174000/process
- **Resposta Esperada** (200 OK):
  ```json
  {
    "id": "123e4567-e89b-12d3-a456-426614174000",
    "orderNumber": "ORD-TEST-101",
    "status": "CALCULATED",
    "createdAt": "2025-05-08T10:00:00",
    "processedAt": "2025-05-08T10:01:30",
    "items": [
      {
        "productId": "PROD-1",
        "productName": "Produto 1",
        "quantity": 2,
        "price": 20.00
      },
      {
        "productId": "PROD-2",
        "productName": "Produto 2",
        "quantity": 1,
        "price": 30.00
      }
    ],
    "totalAmount": 70.00
  }
  ```

### 6. Notificar Sistema Externo

- **Método**: POST
- **URL**: http://localhost:8080/api/orders/{id}/notify
- **Exemplo**: http://localhost:8080/api/orders/123e4567-e89b-12d3-a456-426614174000/notify
- **Resposta Esperada** (200 OK):
  ```json
  {
    "id": "123e4567-e89b-12d3-a456-426614174000",
    "orderNumber": "ORD-TEST-101",
    "status": "NOTIFIED",
    "createdAt": "2025-05-08T10:00:00",
    "processedAt": "2025-05-08T10:01:30",
    "completedAt": "2025-05-08T10:02:15",
    "items": [
      {
        "productId": "PROD-1",
        "productName": "Produto 1",
        "quantity": 2,
        "price": 20.00
      },
      {
        "productId": "PROD-2",
        "productName": "Produto 2",
        "quantity": 1,
        "price": 30.00
      }
    ],
    "totalAmount": 70.00
  }
  ```

## Cenários de Teste

### Cenário 1: Fluxo Completo de Pedido

1. Criar um novo pedido (POST /api/orders)
2. Verificar o status inicial (GET /api/orders/{orderNumber}/status)
3. Aguardar alguns segundos para o processamento automático
4. Verificar o status atualizado (GET /api/orders/{orderNumber}/status)
5. Obter os detalhes completos do pedido (GET /api/orders/{orderNumber})

### Cenário 2: Tratamento de Erros

1. Tentar criar um pedido com número duplicado
   ```json
   {
     "orderNumber": "ORD-TEST-101",
     "items": [
       {
         "productId": "PROD-1",
         "quantity": 2
       }
     ]
   }
   ```
   Resposta esperada: 400 Bad Request com mensagem de erro

2. Tentar obter um pedido inexistente
   - GET http://localhost:8080/api/orders/PEDIDO-INEXISTENTE
   - Resposta esperada: 404 Not Found

### Cenário 3: Alta Volumetria

Para testar a capacidade de alta volumetria, você pode criar um script que envia múltiplos pedidos em sequência:

```javascript
const orderCount = 10;
const baseOrderNumber = "BULK-ORDER-";

for (let i = 1; i <= orderCount; i++) {
    const orderNumber = baseOrderNumber + i;
    
    pm.sendRequest({
        url: pm.variables.get("baseUrl") + "/api/orders",
        method: "POST",
        header: {
            "Content-Type": "application/json"
        },
        body: {
            mode: "raw",
            raw: JSON.stringify({
                orderNumber: orderNumber,
                items: [
                    {
                        productId: "PROD-1",
                        quantity: Math.floor(Math.random() * 5) + 1
                    },
                    {
                        productId: "PROD-2",
                        quantity: Math.floor(Math.random() * 3) + 1
                    }
                ]
            })
        }
    }, function (err, res) {
        console.log(orderNumber + " - Status: " + res.code);
    });
}
```

## Exemplos de Payloads JSON

### Criar Pedido Simples
```json
{
  "orderNumber": "ORD-SIMPLE-001",
  "items": [
    {
      "productId": "PROD-1",
      "quantity": 1
    }
  ]
}
```

### Criar Pedido Complexo
```json
{
  "orderNumber": "ORD-COMPLEX-001",
  "items": [
    {
      "productId": "PROD-1",
      "quantity": 3
    },
    {
      "productId": "PROD-2",
      "quantity": 2
    },
    {
      "productId": "PROD-3",
      "quantity": 5
    },
    {
      "productId": "PROD-4",
      "quantity": 1
    }
  ]
}
```

## Notas Importantes

1. **Perfil Mock**: Ao usar o perfil mock, os serviços externos são simulados e retornam dados fictícios. Isso permite testar a aplicação sem dependências externas.

2. **Processamento Assíncrono**: O processamento de pedidos é assíncrono. Após criar um pedido, ele será processado automaticamente em segundo plano. Você pode verificar o status do pedido para acompanhar seu progresso.

3. **Agendadores**: A aplicação possui agendadores que executam periodicamente para processar pedidos pendentes, notificar sistemas externos e recuperar pedidos com erro. Você pode observar esses processos nos logs da aplicação.

4. **Cache**: A aplicação utiliza cache para otimizar o desempenho. Algumas operações podem retornar resultados em cache.

5. **Resiliência**: A aplicação implementa padrões de resiliência como circuit breaker, bulkhead e retry. No perfil mock, esses mecanismos estão configurados para demonstração.
