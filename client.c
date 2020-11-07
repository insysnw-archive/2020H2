#include "stdio.h"
#include "stdlib.h"
#include "sys/types.h"
#include "sys/socket.h"
#include "string.h"
#include "netinet/in.h"
#include "netdb.h"
#include <gtk/gtk.h>
#include <pthread.h>

#define PORT 8888
#define BUF_SIZE 2000

GtkEntry *textEntry;
GtkTextView *textArea;
GtkTextBuffer *textBuffer;
GtkTextIter iter;
GtkTextMark *textMark;

int sockfd;
struct sockaddr_in addr;

void sendText(GtkButton *sendButton, gpointer data)
{
  int ret;
  int size = gtk_entry_get_text_length(textEntry);
  char *message = (char *)malloc(size);

  strcpy(message, gtk_entry_get_text(textEntry));

  ret = sendto(sockfd, message, size, 0, (struct sockaddr *)&addr, sizeof(addr));
  if (ret < 0)
  {
    printf("Error sending data!\n\t-%s", message);
  }
  gtk_entry_set_text(textEntry, "");
  free(message);
}

void *launchGUI()
{
  GtkWindow *window;
  GtkButton *sendButton;
  GtkTable *table;
  GtkScrolledWindow *scrolledWindow;
  gtk_init(NULL, NULL);

  window = gtk_window_new(GTK_WINDOW_TOPLEVEL);
  gtk_window_set_default_size(window, 600, 800);
  g_signal_connect(window, "destroy", G_CALLBACK(gtk_main_quit), NULL);

  textArea = gtk_text_view_new();
  gtk_text_view_set_editable(GTK_TEXT_VIEW(textArea), FALSE);
  gtk_text_view_set_cursor_visible(GTK_TEXT_VIEW(textArea), FALSE);

  table = gtk_table_new(20, 20, FALSE);
  scrolledWindow = gtk_scrolled_window_new(NULL, NULL);
  textEntry = gtk_entry_new();
  sendButton = gtk_button_new_with_label("Send");

  textBuffer = gtk_text_view_get_buffer(GTK_TEXT_VIEW(textArea));
  gtk_text_buffer_get_iter_at_offset(textBuffer, &iter, 0);
  textMark = gtk_text_mark_new("end", FALSE);
  gtk_text_buffer_add_mark(textBuffer, textMark, &iter);

  gtk_container_add(GTK_CONTAINER(scrolledWindow), textArea);
  gtk_table_attach_defaults(GTK_TABLE(table), scrolledWindow, 0, 20, 0, 19);
  gtk_table_attach_defaults(GTK_TABLE(table), textEntry, 0, 19, 19, 20);
  gtk_table_attach_defaults(GTK_TABLE(table), sendButton, 19, 20, 19, 20);

  gtk_container_add(GTK_CONTAINER(window), table);

  g_signal_connect(sendButton, "clicked", G_CALLBACK(sendText), NULL);

  gtk_widget_show_all(window);
  gtk_main();

  exit(EXIT_SUCCESS);
}

int main(int argc, char **argv)
{

  pthread_t guiThread;

  if (pthread_create(&guiThread, NULL, launchGUI, NULL))
  {
    fprintf(stderr, "Error creating thread\n");
    return 1;
  }

  struct sockaddr_in cl_addr;
  int ret;
  char buffer[BUF_SIZE];
  struct hostent *server;
  char *serverAddr;

  if (argc < 2)
  {
    printf("usage: client < ip address >\n");
    exit(1);
  }

  serverAddr = argv[1];

  sockfd = socket(AF_INET, SOCK_STREAM, 0);
  if (sockfd < 0)
  {
    printf("Error creating socket!\n");
    exit(1);
  }
  printf("Socket created...\n");

  memset(&addr, 0, sizeof(addr));
  addr.sin_family = AF_INET;
  addr.sin_addr.s_addr = inet_addr(serverAddr);
  addr.sin_port = PORT;

  ret = connect(sockfd, (struct sockaddr *)&addr, sizeof(addr));
  if (ret < 0)
  {
    printf("Error connecting to the server!\n");
    exit(1);
  }
  printf("Connected to the server...\n");

  memset(buffer, 0, BUF_SIZE);

  while (textArea == NULL && textBuffer == NULL)
  {
    sleep(1);
  }
  while (1)
  {
    memset(buffer, 0, BUF_SIZE);
    ret = recv(sockfd, buffer, BUF_SIZE, 0);
    if (ret < 0)
    {
      printf("Error receiving data!\n");
    }
    else
    {
      if (ret == 0)
        break; //If 0 bytes were sent, fix it so the client won't be able to send 0 bytes
      //printf("Received: ");
      //fputs(buffer, stdout);
      strcat(buffer, "\n");
      gtk_text_buffer_insert(GTK_TEXT_BUFFER(textBuffer), &iter, buffer, -1);
      gtk_text_view_scroll_mark_onscreen(GTK_TEXT_VIEW(textArea), textMark);
      //printf("\n");
    }
  }

  if (pthread_join(guiThread, NULL))
  {
    fprintf(stderr, "Error joining thread\n");
    return 2;
  }

  return 0;
}
