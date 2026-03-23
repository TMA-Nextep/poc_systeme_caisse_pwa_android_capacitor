import socket

# Configure l'IP de ton PC et le port de l'imprimante
HOST = '0.0.0.0' # Écoute sur toutes les interfaces
PORT = 9100

with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
    s.bind((HOST, PORT))
    s.listen()
    print(f"L'imprimante fantôme écoute sur le port {PORT}...")
    while True:
        conn, addr = s.accept()
        with conn:
            print(f"Connexion reçue de {addr} !")
            # On reçoit les données mais on ne fait rien avec
            data = conn.recv(1024)
            if data:
                print(f"Données reçues (ESC/POS) : {data}")