<?php
/**
 * includes/pie.php — Cierre de </main>, pie visual y el único <script> del
 * sitio. Cualquier página que abra includes/cabecera.php debe cerrar con este
 * archivo: son un par, no dos includes independientes.
 */
?>
</main>

<footer class="pie">
  <span>SVIS — Sistema de Votaciones y Encuestas Institucionales Seguras</span>
  <span>Centro de Formación SENA</span>
</footer>

<?php if (!empty($esAdminActual)): ?>
  </div><!-- /.panel-columna -->
</div><!-- /.panel-app -->
<?php endif; ?>

<script src="<?= BASE_URL ?>/js/app.js"></script>
</body>
</html>
